package com.bennybarak.scoops.rotter_scoops.net

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
 */
@SuppressLint("SetJavaScriptEnabled")
object RotterGated {
    private const val TAG = "RotterGated"
    private const val BASE = "https://rotter.net/cgi-bin/forum/dcboard.cgi"
    private const val BOOT = "$BASE?az=login"

    lateinit var appContext: Context
    private val scope: CoroutineScope = MainScope()

    private var webView: WebView? = null
    private var booting: CompletableDeferred<Boolean>? = null
    private var ready = false

    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val seq = AtomicInteger()

    /** Receives each fetch's result from page JavaScript. */
    class Bridge {
        @JavascriptInterface
        fun post(id: String, json: String) {
            pending.remove(id)?.complete(json)
        }
    }

    /**
     * Boot (once) a hidden webview parked on a gated rotter page, so its JS
     * context has cleared Cloudflare and holds the cookie jar.
     */
    private suspend fun ensure(): Boolean = withContext(Dispatchers.Main) {
        if (ready) return@withContext true
        booting?.let { return@withContext it.await() }
        val done = CompletableDeferred<Boolean>()
        booting = done

        val wv = WebView(appContext)
        webView = wv
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = ROTTER_USER_AGENT
        wv.addJavascriptInterface(Bridge(), "ScoopsBridge")
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val title = view.title?.lowercase() ?: ""
                // Cloudflare interstitial still up — wait for it to redirect.
                if (title.contains("just a moment") || title.contains("attention required")) return
                done.complete(true)
            }
        }
        wv.loadUrl(BOOT)
        // Give up waiting after 40s and try anyway, as the Flutter build did.
        scope.launch {
            delay(40_000)
            done.complete(true)
        }
        val ok = done.await()
        ready = ok
        booting = null
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
        val id = seq.incrementAndGet().toString()
        val result = CompletableDeferred<String>()
        pending[id] = result
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
              return {status: r.status, redirected: r.redirected, b64: btoa(bin)};
            })().then(
              function(v) { ScoopsBridge.post(${JSONObject.quote(id)}, JSON.stringify(v)); },
              function(e) { ScoopsBridge.post(${JSONObject.quote(id)}, JSON.stringify({error: String(e)})); }
            );
        """.trimIndent()
        withContext(Dispatchers.Main) { webView?.evaluateJavascript(js, null) }
        val raw = withTimeoutOrNull(60_000) { result.await() }
        pending.remove(id)
        if (raw == null) {
            Log.d(TAG, "fetch timed out for $method $url")
            return null
        }
        return try {
            val o = JSONObject(raw)
            if (o.has("error")) {
                Log.d(TAG, "fetch error: ${o.optString("error")} for $method $url")
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
