package com.bennybarak.scoops.rotter_scoops.data

import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

// All stores are read and written on the main thread (like the Flutter build's
// single isolate); their observable fields are Compose snapshot state so the UI
// that reads them redraws on change.

enum class ThemeMode { system, light, dark }

/**
 * Per-thread history only matters while a thread can still be in the feed.
 * Thread ids grow (~100 a day); forget ids more than [HISTORY_SPAN] behind
 * the newest one kept, so the stored JSON — rewritten on every change and
 * loaded at launch — stays bounded (about six months).
 */
private const val HISTORY_SPAN = 20_000L

internal fun pruneOld(ids: MutableSet<String>) {
    if (ids.size < 2_000) return
    val newest = ids.maxOfOrNull { it.toLongOrNull() ?: 0L } ?: return
    ids.removeAll { (it.toLongOrNull() ?: newest) < newest - HISTORY_SPAN }
}

/**
 * Selectable colour themes (stored under the old "accent" key). Amber is the
 * classic look; every other theme also tints the app's surfaces.
 */
enum class Accent(val seed: Long) {
    amber(0xFFF57C00),
    red(0xFFD32030),
    blue(0xFF1565C0),
    green(0xFF2E7D32),
    purple(0xFF6A3DE8),
    graphite(0xFF4A5160),
    lavender(0xFF6750A4),
}

/**
 * How the scoops list is ordered. `lastComment` is rotter's own default (most
 * recently active first); `postTime` orders strictly by when each was posted.
 */
enum class SortMode { lastComment, postTime }

private fun Double.round2() = (this * 100).roundToInt() / 100.0

/**
 * App-wide preferences (theme mode, accent, language…), persisted across launches.
 *
 * Language default is **Hebrew** regardless of device locale — `null` means
 * "follow device", and the stored default on first launch is Hebrew.
 */
object SettingsController {
    private const val MODE_KEY = "theme_mode"
    private const val ACCENT_KEY = "accent"
    private const val LOCALE_KEY = "locale" // 'system' | 'he' | 'en'
    private const val SCALE_KEY = "text_scale" // the old single setting: the default for both below
    private const val LIST_SCALE_KEY = "list_text_scale"
    private const val ARTICLE_SCALE_KEY = "article_text_scale"
    private const val SORT_KEY = "sort_mode" // 'lastComment' | 'postTime'
    private const val DENSITY_KEY = "thread_density"
    private const val PREDICTIVE_BACK_KEY = "predictive_back"

    /** Readable text-size range, applied on top of the device's own scaling. */
    const val MIN_SCALE = 0.9
    const val MAX_SCALE = 1.5

    /** Multiplier on the in-thread comment padding/gaps (lower = tighter). */
    const val MIN_DENSITY = 0.4
    const val MAX_DENSITY = 1.2

    @set:JvmName("assignMode")
    var mode by mutableStateOf(ThemeMode.system); private set
    @set:JvmName("assignAccent")
    var accent by mutableStateOf(Accent.amber); private set

    /** `null` → follow device locale; otherwise a forced language code. */
    @set:JvmName("assignLocale")
    var locale by mutableStateOf<String?>("he"); private set

    /**
     * Text size factors (1.0 = default) for the scoops list and for a thread's
     * post and comments, applied on top of the device's own font size.
     */
    @set:JvmName("assignListScale")
    var listScale by mutableDoubleStateOf(1.0); private set

    @set:JvmName("assignArticleScale")
    var articleScale by mutableDoubleStateOf(1.0); private set

    /** Scoops list ordering; defaults to rotter's own "last comment" order. */
    @set:JvmName("assignSortMode")
    var sortMode by mutableStateOf(SortMode.lastComment); private set

    /** In-thread comment spacing factor (1.0 = roomy; default is tighter). */
    @set:JvmName("assignThreadDensity")
    var threadDensity by mutableDoubleStateOf(0.7); private set

    /**
     * Back gesture: true = system predictive back (drag peeks the previous
     * screen); false = the classic fade + slide-up transition.
     */
    @set:JvmName("assignPredictiveBack")
    var predictiveBack by mutableStateOf(true); private set

    fun load() {
        mode = when (Prefs.getString(MODE_KEY)) {
            "light" -> ThemeMode.light
            "dark" -> ThemeMode.dark
            else -> ThemeMode.system
        }
        accent = Accent.entries.firstOrNull { it.name == Prefs.getString(ACCENT_KEY) } ?: Accent.amber
        // Default to Hebrew on first launch (no stored value).
        locale = when (Prefs.getString(LOCALE_KEY)) {
            "system" -> null
            "en" -> "en"
            else -> "he"
        }
        val legacy = Prefs.getDouble(SCALE_KEY)
        (Prefs.getDouble(LIST_SCALE_KEY) ?: legacy)?.let { listScale = it.coerceIn(MIN_SCALE, MAX_SCALE) }
        (Prefs.getDouble(ARTICLE_SCALE_KEY) ?: legacy)?.let { articleScale = it.coerceIn(MIN_SCALE, MAX_SCALE) }
        sortMode = if (Prefs.getString(SORT_KEY) == "postTime") SortMode.postTime else SortMode.lastComment
        Prefs.getDouble(DENSITY_KEY)?.let { threadDensity = it.coerceIn(MIN_DENSITY, MAX_DENSITY) }
        predictiveBack = Prefs.getBool(PREDICTIVE_BACK_KEY) ?: true
    }

    fun setPredictiveBack(v: Boolean) {
        predictiveBack = v
        Prefs.setBool(PREDICTIVE_BACK_KEY, v)
    }

    fun setThreadDensity(v: Double) {
        val clamped = v.coerceIn(MIN_DENSITY, MAX_DENSITY).round2()
        threadDensity = clamped
        Prefs.setDouble(DENSITY_KEY, clamped)
    }

    private val sortListeners = ArrayList<() -> Unit>()
    fun addSortListener(l: () -> Unit) = sortListeners.add(l)
    fun removeSortListener(l: () -> Unit) = sortListeners.remove(l)
    internal val sortListenerCount get() = sortListeners.size

    fun setSortMode(m: SortMode) {
        val changed = m != sortMode
        sortMode = m
        Prefs.setString(SORT_KEY, m.name)
        if (changed) sortListeners.toList().forEach { it() }
    }

    fun setMode(m: ThemeMode) {
        mode = m
        Prefs.setString(MODE_KEY, m.name)
    }

    fun setAccent(a: Accent) {
        accent = a
        Prefs.setString(ACCENT_KEY, a.name)
    }

    fun setLocale(l: String?) {
        locale = l
        Prefs.setString(LOCALE_KEY, l ?: "system")
    }

    fun setListScale(v: Double) {
        val clamped = v.coerceIn(MIN_SCALE, MAX_SCALE).round2()
        listScale = clamped
        Prefs.setDouble(LIST_SCALE_KEY, clamped)
    }

    fun setArticleScale(v: Double) {
        val clamped = v.coerceIn(MIN_SCALE, MAX_SCALE).round2()
        articleScale = clamped
        Prefs.setDouble(ARTICLE_SCALE_KEY, clamped)
    }
}

/**
 * Tracks the rotter.net session and holds the saved credentials.
 *
 * The session cookie lives in the shared WebView cookie jar (so Cloudflare just
 * works). The username + password are persisted in the Keystore so the app can
 * sign in silently — the user only re-enters credentials after an explicit
 * sign-out or if the saved ones stop working.
 */
object AuthService {
    private const val LOGGED_IN_KEY = "logged_in"
    private const val USER_KEY = "username"
    private const val SEC_USER_KEY = "rotter_user"
    private const val SEC_PASS_KEY = "rotter_pass"

    var loggedIn by mutableStateOf(false); private set
    var username by mutableStateOf<String?>(null); private set

    fun load() {
        loggedIn = Prefs.getBool(LOGGED_IN_KEY) ?: false
        username = Prefs.getString(USER_KEY)
    }

    data class Credentials(val user: String, val pass: String)

    /** The saved credentials, or null if none stored. */
    suspend fun credentials(): Credentials? = withContext(Dispatchers.IO) {
        val u = SecureStore.read(SEC_USER_KEY)
        val p = SecureStore.read(SEC_PASS_KEY)
        if (u.isNullOrEmpty() || p.isNullOrEmpty()) null else Credentials(u, p)
    }

    suspend fun saveCredentials(user: String, pass: String) = withContext(Dispatchers.IO) {
        SecureStore.write(SEC_USER_KEY, user)
        SecureStore.write(SEC_PASS_KEY, pass)
    }

    fun markLoggedIn(user: String?) {
        loggedIn = true
        username = user
        Prefs.setBool(LOGGED_IN_KEY, true)
        if (user != null) Prefs.setString(USER_KEY, user)
    }

    /**
     * User-initiated sign-out: forget the saved credentials and end the rotter
     * session by clearing the shared WebView cookie jar (otherwise the next
     * compose would still post as the previous account). "My replies" is
     * cleared too — it isn't namespaced by account.
     */
    suspend fun signOut() {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (_: Exception) { /* best-effort */ }
        // Its page context was cleared with the cookies: boot a fresh one next time.
        com.bennybarak.scoops.rotter_scoops.net.RotterGated.reset()
        withContext(Dispatchers.IO) {
            SecureStore.delete(SEC_USER_KEY)
            SecureStore.delete(SEC_PASS_KEY)
        }
        MyRepliesStore.clear()
        clearLocal()
    }

    /**
     * Sync to "logged out" when rotter itself shows a logged-out page (session
     * expired / logged out). Does not touch cookies.
     */
    fun markLoggedOut() = clearLocal()

    private fun clearLocal() {
        loggedIn = false
        username = null
        Prefs.remove(LOGGED_IN_KEY)
        Prefs.remove(USER_KEY)
    }
}

/**
 * Tracks the scoop threads the user has replied to (persisted), so the list and
 * the thread can mark them with a green side. Which *comment* is the user's own
 * is decided live by matching the author to the logged-in username.
 */
object MyRepliesStore {
    private const val KEY = "my_reply_threads"
    private val threads = mutableStateMapOf<String, Unit>()

    fun load() {
        threads.clear()
        Prefs.getString(KEY)?.let { raw ->
            val a = JSONArray(raw)
            for (i in 0 until a.length()) threads[a.getString(i)] = Unit
        }
    }

    fun replied(threadId: String) = threads.containsKey(threadId)

    fun add(threadId: String) {
        if (threads.containsKey(threadId)) return
        threads[threadId] = Unit
        Prefs.setString(KEY, JSONArray(threads.keys.toList()).toString())
    }

    /** Forget everything on sign-out — the set isn't namespaced by account. */
    fun clear() {
        if (threads.isEmpty()) return
        threads.clear()
        Prefs.remove(KEY)
    }
}

/**
 * Tracks which scoop threads have been read.
 *
 * For each read thread we remember the **reply count we last saw**. A thread is
 * "read" while its current reply count is still ≤ the seen count; once new
 * comments arrive it flips back to unread *and* is flagged [isNew] so the list
 * can show a "new comments" pill.
 */
object ReadStore {
    private const val KEY = "read_threads" // JSON: { id: seenReplyCount }
    private const val NEW_KEY = "new_threads" // JSON: [ ids with new comments ]

    /**
     * Sentinel: "read, but the reply-count baseline isn't known yet" (e.g.
     * marked read via swipe while offline). Stays read; the next metadata
     * arrival sets the real baseline.
     */
    const val PENDING = -1

    private val seen = mutableStateMapOf<String, Int>()
    private val fresh = mutableStateMapOf<String, Unit>()

    fun load() {
        seen.clear()
        fresh.clear()
        Prefs.getString(KEY)?.let { raw ->
            val o = JSONObject(raw)
            for (k in o.keys()) if (!o.isNull(k)) seen[k] = o.optInt(k, PENDING)
        }
        Prefs.getString(NEW_KEY)?.let { raw ->
            val a = JSONArray(raw)
            for (i in 0 until a.length()) fresh[a.getString(i)] = Unit
        }
    }

    fun isRead(id: String) = seen.containsKey(id)
    fun seenCount(id: String) = seen[id]
    fun isPending(id: String) = seen[id] == PENDING

    /** True for threads that were read and have since gained new comments. */
    fun isNew(id: String) = fresh.containsKey(id)

    fun markRead(id: String, replyCount: Int) {
        seen[id] = replyCount
        fresh.remove(id) // opening/reading clears the "new" flag
        save()
    }

    /**
     * Mark every given thread read at once. Threads whose baseline we don't
     * know are set [PENDING] — the next sweep fills in the real reply count
     * without flagging them new.
     */
    fun markAllRead(ids: Iterable<String>) {
        var changed = false
        for (id in ids) {
            if (!seen.containsKey(id)) {
                seen[id] = PENDING
                changed = true
            }
            if (fresh.remove(id) != null) changed = true
        }
        if (changed) save()
    }

    fun markUnread(id: String) {
        val hadSeen = seen.remove(id) != null
        val hadNew = fresh.remove(id) != null
        if (hadSeen || hadNew) save()
    }

    /**
     * Apply a reply count just observed on rotter: resolve a pending baseline,
     * or flag new comments on a read thread whose count grew.
     */
    fun reconcile(id: String, replyCount: Int) {
        if (isPending(id)) {
            markRead(id, replyCount)
        } else if (isRead(id) && replyCount > (seenCount(id) ?: replyCount)) {
            markNewComments(id)
        }
    }

    /** A read thread gained new comments: drop it back to unread and flag it "new". */
    fun markNewComments(id: String) {
        seen.remove(id)
        fresh[id] = Unit
        save()
    }

    private fun save() {
        pruneOld(seen.keys)
        Prefs.setString(KEY, JSONObject(seen.toMap() as Map<*, *>).toString())
        Prefs.setString(NEW_KEY, JSONArray(fresh.keys.toList()).toString())
    }
}

/**
 * A persisted set of scoops — used twice: bookmarks ([saved]) and threads the
 * user follows ([followed]). Keeps the headline and link, not just the id, so
 * they outlive the rolling RSS feed.
 */
class SavedStore private constructor(private val key: String) {
    companion object {
        val saved = SavedStore("saved_scoops")
        val followed = SavedStore("followed_scoops")
    }

    private val entries = LinkedHashMap<String, Scoop>()
    private var version by mutableIntStateOf(0)

    fun load() {
        entries.clear()
        val raw = Prefs.getString(key) ?: return
        val o = JSONObject(raw)
        for (k in o.keys()) entries[k] = Scoop.fromJson(o.getJSONObject(k))
        version++
    }

    val scoops: List<Scoop> get() = version.let { entries.values.toList() }
    fun contains(id: String): Boolean = version.let { entries.containsKey(id) }

    fun toggle(s: Scoop) {
        if (entries.remove(s.id) == null) entries[s.id] = s
        version++
        val o = JSONObject()
        for ((k, v) in entries) o.put(k, v.toJson())
        Prefs.setString(key, o.toString())
    }
}

/**
 * Per-thread reading history: the highest message number seen (so the next
 * visit can mark newer comments "new") and where the reader left off (so the
 * thread can offer to resume there). Message numbers only, never bodies.
 */
object ReadingStore {
    private const val KEY = "thread_reading_history"
    // { id: {"latest": int?, "position": int?} }
    private val visits = HashMap<String, HashMap<String, Int>>()

    fun load() {
        visits.clear()
        val raw = Prefs.getString(KEY) ?: return
        val o = JSONObject(raw)
        for (k in o.keys()) {
            val v = o.optJSONObject(k) ?: continue
            visits[k] = HashMap<String, Int>().apply { for (f in v.keys()) if (!v.isNull(f)) put(f, v.optInt(f)) }
        }
    }

    fun latestMessage(id: String): Int? = visits[id]?.get("latest")
    fun position(id: String): Int? = visits[id]?.get("position")

    fun recordVisit(id: String, latestMessage: Int) {
        val v = visits.getOrPut(id) { HashMap() }
        val prev = v["latest"] ?: 0
        v["latest"] = if (latestMessage > prev) latestMessage else prev
        save()
    }

    fun rememberPosition(id: String, number: Int) {
        if (visits[id]?.get("position") == number) return
        visits.getOrPut(id) { HashMap() }["position"] = number
        save()
    }

    private fun save() {
        pruneOld(visits.keys)
        val o = JSONObject()
        for ((k, v) in visits) o.put(k, JSONObject(v as Map<*, *>))
        Prefs.setString(KEY, o.toString())
    }
}

/**
 * Unsent composer text, kept per target (a reply, an edit, or a new thread) so
 * nothing typed is lost to a back-swipe or a failed post. Saved as it's typed,
 * restored when the same target is opened again, cleared once the post
 * succeeds or the user explicitly discards it.
 */
object DraftStore {
    private const val KEY = "composer_drafts"
    // { target: {"subject": ..., "body": ...} }
    private val drafts = LinkedHashMap<String, Pair<String, String>>()

    fun load() {
        drafts.clear()
        val raw = Prefs.getString(KEY) ?: return
        val o = JSONObject(raw)
        for (k in o.keys()) {
            val d = o.getJSONObject(k)
            drafts[k] = d.optString("subject", "") to d.optString("body", "")
        }
    }

    fun draft(target: String): Pair<String, String>? = drafts[target]

    fun save(target: String, subject: String, body: String) {
        // An emptied composer is not a draft worth restoring.
        if (subject.isBlank() && body.isBlank()) return clear(target)
        if (drafts[target] == (subject to body)) return
        drafts[target] = subject to body
        persist()
    }

    fun clear(target: String) {
        if (drafts.remove(target) != null) persist()
    }

    private fun persist() {
        val o = JSONObject()
        for ((k, v) in drafts) o.put(k, JSONObject().put("subject", v.first).put("body", v.second))
        Prefs.setString(KEY, o.toString())
    }
}

/**
 * The language a thread summary is written in — chosen independently of the
 * interface.
 */
enum class SummaryLanguage { followApp, hebrew, english }

/**
 * Settings for the optional LLM thread summary. The API key is a credential and
 * lives in the Keystore (like the rotter password); the enabled flag, model,
 * endpoint and language are ordinary preferences.
 */
object AIStore {
    /**
     * Editable rather than hardcoded: model identifiers change often, and a
     * wrong one should be a line of text to correct, not a rebuild.
     */
    const val DEFAULT_MODEL = "gpt-5.6-luna"

    /** The API root — configurable for OpenAI-compatible gateways. */
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"

    private const val ENABLED_KEY = "ai_summaries_enabled"
    private const val MODEL_KEY = "ai_model"
    private const val BASE_URL_KEY = "ai_base_url"
    private const val LANGUAGE_KEY = "ai_summary_language"
    private const val SEC_KEY = "openai_api_key"

    @set:JvmName("assignEnabled")
    var enabled by mutableStateOf(false); private set
    @set:JvmName("assignModel")
    var model by mutableStateOf(DEFAULT_MODEL); private set
    @set:JvmName("assignBaseUrl")
    var baseUrl by mutableStateOf(DEFAULT_BASE_URL); private set
    @set:JvmName("assignLanguage")
    var language by mutableStateOf(SummaryLanguage.followApp); private set

    /** Tracked rather than read from secure storage on demand, so UI can observe. */
    var hasKey by mutableStateOf(false); private set

    /** Summaries are only offered when there is actually something to call with. */
    val isReady get() = enabled && hasKey

    fun load() {
        enabled = Prefs.getBool(ENABLED_KEY) ?: false
        model = Prefs.getString(MODEL_KEY) ?: DEFAULT_MODEL
        baseUrl = Prefs.getString(BASE_URL_KEY) ?: DEFAULT_BASE_URL
        language = SummaryLanguage.entries.firstOrNull { it.name == Prefs.getString(LANGUAGE_KEY) }
            ?: SummaryLanguage.followApp
    }

    /** The Keystore half of [load] (blocking crypto, so run off the main thread). */
    suspend fun loadKeyState() {
        val has = apiKey() != null
        withContext(Dispatchers.Main) { hasKey = has }
    }

    suspend fun apiKey(): String? = withContext(Dispatchers.IO) {
        SecureStore.read(SEC_KEY)?.takeIf { it.isNotEmpty() }
    }

    suspend fun setKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return clearKey()
        withContext(Dispatchers.IO) { SecureStore.write(SEC_KEY, trimmed) }
        hasKey = true
    }

    suspend fun clearKey() {
        withContext(Dispatchers.IO) { SecureStore.delete(SEC_KEY) }
        hasKey = false
        setEnabled(false)
    }

    fun setEnabled(v: Boolean) {
        enabled = v
        Prefs.setBool(ENABLED_KEY, v)
    }

    fun setModel(v: String) {
        model = v.trim().ifEmpty { DEFAULT_MODEL }
        Prefs.setString(MODEL_KEY, model)
    }

    fun setBaseUrl(v: String) {
        baseUrl = v.trim().ifEmpty { DEFAULT_BASE_URL }
        Prefs.setString(BASE_URL_KEY, baseUrl)
    }

    fun setLanguage(v: SummaryLanguage) {
        language = v
        Prefs.setString(LANGUAGE_KEY, v.name)
    }

    /**
     * The language name given to the model (the prompt itself is English).
     * [appLocale] is the in-app language; null means "follow the device".
     */
    fun promptLanguage(appLocale: String?): String = when (language) {
        SummaryLanguage.hebrew -> "Hebrew"
        SummaryLanguage.english -> "English"
        SummaryLanguage.followApp -> {
            val code = appLocale ?: Locale.getDefault().language
            if (code == "he" || code == "iw") "Hebrew" else "English"
        }
    }
}
