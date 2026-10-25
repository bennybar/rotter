package com.bennybarak.scoops.rotter_scoops.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * Reads what a scoop links to, so summaries see it too: most scoops are a
 * line and a link to the Telegram post, X post or news article that carries
 * the substance. Telegram's embed page, X's embed data and a news page's own
 * summary (og:description) all read without signing in.
 */
object LinkReader {
    private const val MAX_LINKS = 2
    private const val MAX_CHARS = 600

    /** A news page's summary is in its <head>; don't download the whole article. */
    private const val MAX_PAGE_BYTES = 200_000L

    /** url → its text ("" when it couldn't be read), least recently used dropped first. */
    private val cache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 500
    }

    private val telegramRe = Regex("""^https?://(?:www\.)?t\.me/(?:s/)?([A-Za-z0-9_]+)/(\d+)""")
    private val tweetRe = Regex("""^https?://(?:www\.|mobile\.)?(?:x|twitter)\.com/[^/]+/status/(\d+)""")
    private val mediaRe = Regex("""\.(?:jpe?g|png|gif|webp|mp4|mp3|pdf)(?:\?|$)""", RegexOption.IGNORE_CASE)

    /** Ads, image hosts and chats that have nothing to read without an account. */
    private val skipHosts = listOf(
        "rotter.net", "taboola.com", "doubleclick.net", "googlesyndication.com",
        "whatsapp.com", "imageshack.com", "ibb.co", "imgur.com",
    )
    private val ws = Regex("""\s+""")
    private val tco = Regex("""https://t\.co/\S+""")

    /** The external links in a post's body worth reading, in order. */
    fun links(bodyHtml: String): List<String> =
        Jsoup.parseBodyFragment(bodyHtml).select("a[href], iframe[src]")
            .map { it.attr("href").ifEmpty { it.attr("src") }.trim() }
            // One Telegram post shows up as both its embed and a "view in Telegram" link.
            .map { url -> telegramRe.find(url)?.let { "https://t.me/${it.groupValues[1]}/${it.groupValues[2]}" } ?: url }
            .filter { url ->
                val host = url.substringAfter("://").substringBefore('/').lowercase()
                url.startsWith("http") && !mediaRe.containsMatchIn(url) && skipHosts.none { host.endsWith(it) }
            }
            .distinct()
            .take(MAX_LINKS)

    /**
     * [linked] unless [post] already quotes it (rotter posts often paste the
     * Telegram text in full), so the transcript doesn't spend its room twice.
     */
    fun unlessQuoted(linked: String?, post: String): String? =
        linked?.takeUnless { post.contains(it.take(60).trim()) }

    /** The text behind a post's links, joined; null when none could be read. */
    suspend fun read(bodyHtml: String?): String? {
        if (bodyHtml == null) return null
        val texts = coroutineScope { links(bodyHtml).map { async { readOne(it) } }.awaitAll() }
        return texts.filter { it.isNotBlank() }.distinct().joinToString(" | ").ifEmpty { null }
    }

    private suspend fun readOne(url: String): String {
        synchronized(cache) { cache[url] }?.let { return it }
        val text = try {
            val tg = telegramRe.find(url)
            val tweet = tweetRe.find(url)
            when {
                tg != null -> {
                    val (chan, post) = tg.destructured
                    val html = String(getBytes("https://t.me/$chan/$post?embed=1", 8, mapOf("User-Agent" to ROTTER_USER_AGENT)))
                    Jsoup.parse(html).selectFirst(".tgme_widget_message_text")?.wholeText() ?: ""
                }
                tweet != null -> {
                    val json = String(getBytes("https://cdn.syndication.twimg.com/tweet-result?id=${tweet.groupValues[1]}&token=a", 8))
                    JSONObject(json).optString("text").replace(tco, "")
                }
                else -> pageSummary(url)
            }.replace(ws, " ").trim().take(MAX_CHARS)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            "" // gone, blocked or a blip: the summary goes without it
        }
        synchronized(cache) { cache[url] = text }
        return text
    }

    /** A web page's title and description, from the start of the page. */
    private suspend fun pageSummary(url: String): String = withContext(Dispatchers.IO) {
        val call = Http.client.newCall(Request.Builder().url(url).header("User-Agent", ROTTER_USER_AGENT).build())
        call.timeout().timeout(8, TimeUnit.SECONDS)
        val bytes = call.await().use { res ->
            if (res.code != 200) return@withContext ""
            val source = res.body.source()
            source.request(MAX_PAGE_BYTES)
            source.buffer.readByteArray(minOf(MAX_PAGE_BYTES, source.buffer.size))
        }
        // Jsoup reads the page's own charset declaration (Israeli sites still use windows-1255).
        val doc = Jsoup.parse(bytes.inputStream(), null, url)
        fun meta(q: String) = doc.selectFirst(q)?.attr("content")?.trim().orEmpty()
        val title = meta("meta[property=og:title]").ifEmpty { doc.title() }
        val desc = meta("meta[property=og:description]").ifEmpty { meta("meta[name=description]") }
        listOf(title, desc).filter { it.isNotEmpty() }.joinToString(" — ")
    }
}
