package com.bennybarak.scoops.rotter_scoops.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/**
 * One User-Agent used everywhere we talk to rotter through a browser context
 * (the gated webview) and for Telegram embeds. Cloudflare ties `cf_clearance`
 * to the UA that earned it, so keep them identical.
 */
const val ROTTER_USER_AGENT =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

object Http {
    /**
     * Shared client. The card-metadata backfill runs 20 requests at once against
     * one host, so the per-host cap (OkHttp defaults to 5) must be above that or
     * the queue silently serializes.
     */
    val client: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .build()
}

/** A non-200 from rotter. A 404 means the thread doesn't exist — removed by a
 * moderator, or so new rotter hasn't generated its static page yet. */
class RotterHttpException(val status: Int, val url: String) : Exception("HTTP $status for $url") {
    val isNotFound get() = status == 404
}

/** Await an OkHttp call; cancelling the coroutine cancels the request. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { _, _, _ -> response.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}

/** GET [url]'s raw bytes with a whole-call timeout; non-200 throws [RotterHttpException]. */
suspend fun getBytes(
    url: String,
    timeoutSeconds: Long = 15,
    headers: Map<String, String> = emptyMap(),
): ByteArray = withContext(Dispatchers.IO) {
    val req = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
    val call = Http.client.newCall(req)
    call.timeout().timeout(timeoutSeconds, TimeUnit.SECONDS)
    call.await().use { res ->
        if (res.code != 200) throw RotterHttpException(res.code, url)
        res.body.bytes()
    }
}
