package com.bennybarak.scoops.rotter_scoops.net

import android.util.Log
import android.webkit.CookieManager
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

enum class LoginOutcome {
    success,
    wrongCredentials,
    // Couldn't complete (no Cloudflare clearance, network error). Caller shows
    // an error — there is no webview fallback.
    failed,
}

class LoginResult(val outcome: LoginOutcome, val user: String? = null)

private const val BASE = "https://rotter.net/cgi-bin/forum/dcboard.cgi"

/**
 * Signs in to rotter.net with a **direct cp1255 POST** to `dcboard.cgi`
 * (`cmd=login&az=login` plus the Hebrew username/password fields), NOT by
 * driving rotter's web login page. The request runs through [RotterGated] so
 * it executes inside the Cloudflare-cleared browser context; the session cookie
 * lands in the shared WebView cookie jar that posting reuses.
 */
object RotterLogin {
    private const val TAG = "RotterLogin"

    /**
     * Attempt a sign-in. On success, persists the session + username
     * (credentials are NOT saved here — the caller decides whether to keep them).
     */
    suspend fun attempt(user: String, pass: String): LoginResult {
        // 1. Direct login POST — cp1255 form with the Hebrew field names.
        val body = encodeWin1255Form(
            listOf("cmd" to "login", "az" to "login", "שם-משתמש" to user, "סיסמא" to pass),
        )
        val resp = RotterGated.request(BASE, method = "POST", body = body)
        if (resp == null || resp.status == 403) return LoginResult(LoginOutcome.failed)
        val post = resp.text.lowercase()
        Log.d(TAG, "POST: status=${resp.status} len=${post.length} logout=${post.contains("az=logout")}")
        // The login response (after following its redirect) shows the logout
        // link when sign-in succeeded.
        if (post.contains("az=logout")) return success(user)

        // 2. Fallback verify on the compose page (same persistent webview → the
        // session cookie login set is present). Logged in iff it shows the
        // logout link or the compose textarea.
        val verify = RotterGated.request("$BASE?az=post&forum=scoops1")
            ?: return LoginResult(LoginOutcome.failed)
        val html = verify.text.lowercase()
        Log.d(TAG, "VERIFY: status=${verify.status} logout=${html.contains("az=logout")} textarea=${html.contains("<textarea")}")
        if (html.contains("az=logout") || html.contains("<textarea")) return success(user)
        return LoginResult(LoginOutcome.wrongCredentials)
    }

    private suspend fun success(user: String): LoginResult {
        withContext(Dispatchers.Main) {
            AuthService.markLoggedIn(user)
            // Persist the fresh session cookie now rather than on WebView's schedule.
            CookieManager.getInstance().flush()
        }
        return LoginResult(LoginOutcome.success, user)
    }

    /**
     * Sign in again from the stored credentials; true if signed in afterwards.
     *
     * rotter's session cookie expires while the app is open and nothing says so
     * — the first sign is a form coming back as though we were a guest, which
     * is when this is called. Only credentials that are actually *rejected*
     * clear the signed-in flag; offline / Cloudflare being slow is no evidence
     * about the account, so the last-known state is left alone.
     */
    suspend fun reauthenticate(): Boolean {
        val creds = AuthService.credentials()
        if (creds == null) {
            withContext(Dispatchers.Main) { AuthService.markLoggedOut() }
            return false
        }
        val r = attempt(creds.user, creds.pass)
        if (r.outcome == LoginOutcome.wrongCredentials) {
            withContext(Dispatchers.Main) { AuthService.markLoggedOut() }
        }
        return r.outcome == LoginOutcome.success
    }

    /**
     * Best-effort silent refresh at startup, so the session cookie is fresh
     * before the user reaches for anything that needs it.
     */
    suspend fun refreshSession() {
        if (AuthService.credentials() == null) return
        reauthenticate()
    }
}

enum class PostOutcome { success, notLoggedIn, blocked, error }

/** The current text of a message being edited, read off rotter's edit form. */
class EditDraft(val subject: String, val body: String)

/**
 * Posts / edits on rotter: GET the relevant form only to read its hidden
 * `rand` token, then POST a body built with explicit field names:
 *
 *   `az=a_mesg&rand=…&name=…&om=…&omm=…&forum=scoops1&subject=…&body=…`
 *
 * `az=a_mesg` posts (reply + new thread), `az=e_mesg` edits. A new thread drops
 * name/om/omm and adds `topic_type=0`; editing the root post also sends
 * `topic_type`. All windows-1255 urlencoded, through [RotterGated]. Success =
 * the 302 redirect back to the thread ([GatedResponse.redirected]).
 */
object RotterPost {
    private const val TAG = "RotterPost"

    private fun editUrl(threadId: String, num: Int) = "$BASE?az=edit&forum=scoops1&om=$threadId&omm=$num"

    /** Reply to [threadId] under message [parentNum] (0 = reply to the original post). */
    suspend fun reply(threadId: String, parentNum: Int, subject: String = "", body: String) = post(
        az = "a_mesg",
        getUrl = "$BASE?az=post&forum=scoops1&om=$threadId&omm=$parentNum",
        om = threadId,
        omm = parentNum,
        subject = subject,
        body = body,
    )

    /** Start a new scoop thread with [subject] + [body]. */
    suspend fun newThread(subject: String, body: String) = post(
        az = "a_mesg",
        getUrl = "$BASE?az=post&forum=scoops1",
        subject = subject,
        body = body,
        topicType = true,
    )

    /** Edit the user's own message [num] in [threadId] (0 = the root post). */
    suspend fun edit(threadId: String, num: Int, subject: String, body: String) = post(
        az = "e_mesg",
        getUrl = editUrl(threadId, num),
        om = threadId,
        omm = num,
        subject = subject,
        body = body,
        topicType = num == 0, // topic_type is sent when editing the root
    )

    /**
     * Read the current subject/body of message [num] from rotter's edit form, to
     * pre-fill the composer. Null when it can't be loaded (not signed in, not
     * the author, network).
     */
    suspend fun loadForEdit(threadId: String, num: Int, isRetry: Boolean = false): EditDraft? {
        val r = RotterGated.request(editUrl(threadId, num))
        if (r == null || r.status == 403) return null
        val doc = withContext(Dispatchers.Default) { Jsoup.parse(r.text) }
        val textarea = doc.selectFirst("textarea")
        if (textarea == null) {
            // Same expired-session case as post(), and worse for being silent:
            // the composer would open empty and saving would blank the message.
            if (!isRetry && RotterLogin.reauthenticate()) {
                return loadForEdit(threadId, num, isRetry = true)
            }
            Log.d(TAG, "loadForEdit: no textarea (not signed in / not author?)")
            return null
        }
        val subject = doc.selectFirst("input[name=subject]")?.attr("value")
            ?: doc.selectFirst("input[type=text]")?.attr("value")
            ?: ""
        return EditDraft(subject, textarea.wholeText())
    }

    private suspend fun post(
        az: String,
        getUrl: String,
        om: String? = null, // thread id; null for a new thread (no name/om/omm)
        omm: Int? = null, // reply parent, or the message being edited
        subject: String,
        body: String,
        topicType: Boolean = false,
        isRetry: Boolean = false,
    ): PostOutcome {
        // 1. GET the form, only to read its hidden `rand` token.
        val r1 = RotterGated.request(getUrl) ?: return PostOutcome.error
        val formHtml = r1.text
        if (r1.status == 403 || formHtml.lowercase().contains("just a moment")) return PostOutcome.blocked

        val doc = withContext(Dispatchers.Default) { Jsoup.parse(formHtml) }
        // No textarea → rotter served this as a guest: the session expired, or
        // we may not edit this message. Sign in again and ask for the form once
        // more; only a second refusal is a real "not signed in".
        if (doc.selectFirst("textarea") == null) {
            if (isRetry || !RotterLogin.reauthenticate()) return PostOutcome.notLoggedIn
            return post(az, getUrl, om, omm, subject, body, topicType, isRetry = true)
        }
        val rand = doc.selectFirst("input[name=rand]")?.attr("value")
            ?: doc.selectFirst("input[name=random]")?.attr("value")
        if (rand == null) {
            Log.d(TAG, "no rand token in form (len=${formHtml.length})")
            return PostOutcome.error
        }

        // 2. Build the POST body with the exact field names, in order.
        val user = withContext(Dispatchers.Main) { AuthService.username } ?: ""
        val fields = buildList {
            add("az" to az)
            add("rand" to rand)
            if (om != null) {
                add("name" to user)
                add("om" to om)
                add("omm" to "$omm")
            }
            add("forum" to "scoops1")
            add("subject" to subject)
            add("body" to body)
            if (topicType) add("topic_type" to "0")
        }

        val r2 = RotterGated.request(BASE, method = "POST", body = encodeWin1255Form(fields))
            ?: return PostOutcome.error
        if (r2.status == 403) return PostOutcome.blocked
        Log.d(TAG, "$az: status=${r2.status} redirected=${r2.redirected}")
        // A 302 redirect (to the thread) is success; via fetch that surfaces as
        // `redirected`.
        return if (r2.redirected) PostOutcome.success else PostOutcome.error
    }
}
