package com.bennybarak.scoops.rotter_scoops.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Response from a gated dcboard.cgi request: HTTP status, whether the request
 * followed a redirect (rotter returns 302 → the thread on a successful post),
 * and the raw cp1255 bytes.
 */
class GatedResponse(val status: Int, val redirected: Boolean, val bytes: ByteArray) {
    val text: String by lazy { decodeWin1255(bytes) }
}

/**
 * Runs gated `dcboard.cgi` requests (login / post / reply) as direct
 * dcboard.cgi calls with windows-1255 urlencoded bodies — but EXECUTES them
 * inside a hidden WebView via `fetch()`, so they go out in the exact browser
 * context that already cleared Cloudflare — same cookies, same TLS. A SINGLE
 * persistent webview is reused for every call so the session cookie login sets
 * is visible to the verify + post requests that follow.
 *
 * The webview is pinned to rotter.net (it can't be navigated elsewhere, and a
 * request only runs while it's on rotter.net — the form bodies carry the
 * password), and results come back over a channel only rotter.net pages can
 * post to. If it fails to come up, ends up off-site, or a request errors or
 * times out, it is torn down and booted again on the next request.
 */
@SuppressLint("SetJavaScriptEnabled")
object RotterGated {
    private const val TAG = "RotterGated"
    private const val BASE = "https://rotter.net/cgi-bin/forum/dcboard.cgi"
    private const val BOOT = "$BASE?az=login"
    private const val ORIGIN = "https://rotter.net"

    lateinit var appContext: Context
    private val scope: CoroutineScope = MainScope()

    private var webView: WebView? = null
    private var booting: CompletableDeferred<Boolean>? = null
    private var ready = false

    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()

    private fun isRotter(url: String?): Boolean {
        val u = Uri.parse(url ?: return false)
        val host = u.host ?: return false
        return u.scheme == "https" && (host == "rotter.net" || host.endsWith(".rotter.net"))
    }

    /** A page's message `{"id": …, …}`: hand it to the request waiting on that id. */
    private fun deliver(json: String) {
        val id = try {
            JSONObject(json).optString("id")
        } catch (_: Exception) {
            return
        }
        pending.remove(id)?.complete(json)
    }

    /**
     * Fallback channel for WebViews without origin-restricted message
     * listeners. Request ids are random, so a frame can't guess one to forge
     * a result.
     */
    class Bridge {
        @JavascriptInterface
        fun postMessage(json: String) = deliver(json)
    }

    /** Throw the webview away; the next request boots a fresh one. */
    fun reset() {
        scope.launch {
            ready = false
            webView?.let {
                it.stopLoading()
                it.destroy()
            }
            webView = null
        }
    }

    /**
     * Boot a hidden webview parked on a gated rotter page, so its JS context
     * has cleared Cloudflare and holds the cookie jar.
     */
    private suspend fun ensure(): Boolean = withContext(Dispatchers.Main) {
        if (ready && isRotter(webView?.url)) return@withContext true
        booting?.let { return@withContext it.await() }
        ready = false
        webView?.destroy()
        val done = CompletableDeferred<Boolean>()
        booting = done

        val wv = WebView(appContext)
        webView = wv
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = ROTTER_USER_AGENT
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            // Only rotter.net frames can post results.
            WebViewCompat.addWebMessageListener(wv, "ScoopsBridge", setOf(ORIGIN)) { _, message, _, _, _ ->
                message.data?.let(::deliver)
            }
        } else {
            wv.addJavascriptInterface(Bridge(), "ScoopsBridge")
        }
        var failed = false
        wv.webViewClient = object : WebViewClient() {
            // Pinned: the page can redirect within rotter (Cloudflare's check
            // runs on the same host) but never navigate the webview away.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                !isRotter(request.url.toString())

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    failed = true
                    done.complete(false)
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (failed || !isRotter(url)) return
                val title = view.title?.lowercase() ?: ""
                // Cloudflare interstitial still up — wait for it to redirect.
                if (title.contains("just a moment") || title.contains("attention required")) return
                done.complete(true)
            }
        }
        wv.loadUrl(BOOT)
        // Give up waiting after 40s and try anyway if the page is on rotter
        // (a slow challenge), as the Flutter build did.
        scope.launch {
            delay(40_000)
            done.complete(!failed && isRotter(wv.url))
        }
        val ok = done.await()
        ready = ok
        booting = null
        if (!ok) {
            wv.destroy()
            if (webView === wv) webView = null
        }
        ok
    }

    /**
     * GET or POST [url]. [body], when non-null, is an already %-encoded cp1255
     * urlencoded form string, sent as `application/x-www-form-urlencoded`.
     */
    suspend fun request(url: String, method: String = "GET", body: String? = null): GatedResponse? {
        if (!ensure()) {
            Log.d(TAG, "webview not ready")
            return null
        }
        val id = UUID.randomUUID().toString()
        val result = CompletableDeferred<String>()
        pending[id] = result
        val qid = JSONObject.quote(id)
        val js = """
            (async function() {
              var u = ${JSONObject.quote(url)}, m = ${JSONObject.quote(method)},
                  bd = ${if (body == null) "null" else JSONObject.quote(body)};
              var opts = {method: m, credentials: 'include', redirect: 'follow'};
              if (bd !== null) {
                opts.headers = {'Content-Type': 'application/x-www-form-urlencoded'};
                opts.body = bd;
              }
              var r = await fetch(u, opts);
              var bytes = new Uint8Array(await r.arrayBuffer()), bin = '';
              for (var i = 0; i < bytes.length; i += 0x8000) {
                bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
              }
              return {id: $qid, status: r.status, redirected: r.redirected, b64: btoa(bin)};
            })().then(
              function(v) { ScoopsBridge.postMessage(JSON.stringify(v)); },
              function(e) { ScoopsBridge.postMessage(JSON.stringify({id: $qid, error: String(e)})); }
            );
        """.trimIndent()
        val sent = withContext(Dispatchers.Main) {
            val wv = webView
            // The body can hold the password: only ever run it on rotter.net.
            if (wv == null || !isRotter(wv.url)) {
                false
            } else {
                wv.evaluateJavascript(js, null)
                true
            }
        }
        if (!sent) {
            pending.remove(id)
            reset()
            return null
        }
        val raw = withTimeoutOrNull(60_000) { result.await() }
        pending.remove(id)
        if (raw == null) {
            Log.d(TAG, "fetch timed out")
            reset()
            return null
        }
        return try {
            val o = JSONObject(raw)
            if (o.has("error")) {
                Log.d(TAG, "fetch error: ${o.optString("error")}")
                reset()
                return null
            }
            GatedResponse(
                o.getInt("status"),
                o.optBoolean("redirected", false),
                Base64.decode(o.optString("b64", ""), Base64.DEFAULT),
            )
        } catch (e: Exception) {
            Log.d(TAG, "fetch threw: $e")
            null
        }
    }
}
