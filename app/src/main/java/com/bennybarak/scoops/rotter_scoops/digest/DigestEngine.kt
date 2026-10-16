package com.bennybarak.scoops.rotter_scoops.digest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.DigestItem
import com.bennybarak.scoops.rotter_scoops.net.DigestSource
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.net.SummaryException
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** At most this many thread pages are read beyond the feed (≈ a day and a half of scoops). */
private const val MAX_OLDER = 400

/** A digest made earlier, kept for [DIGEST_KEEP_MS]: what it covered and its text. */
data class KeptDigest(
    val at: Long,
    val minutes: Int,
    val ids: Set<String>,
    val text: String,
    val loadMs: Long = 0, // fetching the posts
    val aiMs: Long = 0, // the model's answer
    val sources: List<DigestSource> = emptyList(), // what each [n] in the text points at
)

/** Digests are kept this long; their scoops aren't summarized again meanwhile. */
const val DIGEST_KEEP_MS = 24 * 60 * 60_000L

/** The scoops posted inside the window, newest first — by posting time. */
fun inWindow(feed: List<Scoop>, minutes: Int, now: Long): List<Scoop> {
    val since = now - minutes * 60_000L
    return feed.filter { (it.published ?: 0L) >= since }.sortedByDescending { it.published }
}

/** True when the feed doesn't reach back to the start of the window. */
fun needsOlder(feed: List<Scoop>, minutes: Int, now: Long): Boolean {
    val oldest = feed.mapNotNull { it.published }.minOrNull() ?: return false
    return oldest > now - minutes * 60_000L
}

/**
 * Makes digests: an AI summary of the scoops POSTED (not last commented on)
 * in a window, from their main posts only — no comments. Digests are kept for
 * a day and runs are incremental: a new one covers only scoops no kept digest
 * has covered yet. One engine for the app ([shared]): the Digest screen shows
 * its state, and the background worker runs it, so a digest keeps going when
 * the app is closed. Its state is written on the main thread only.
 */
class DigestEngine(val now: () -> Long = System::currentTimeMillis) {
    companion object {
        private const val KEY = "digests"
        val shared by lazy { DigestEngine() }
    }

    var running by mutableStateOf(false); private set
    var syncing by mutableStateOf(false); private set // bringing the feed up to date
    var progress by mutableStateOf<Pair<Int, Int>?>(null); private set // posts loaded / total
    var summarizing by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    /** Set when a run found nothing new to summarize. */
    var upToDate by mutableStateOf(false); private set

    /** Kept digests, newest first. */
    var kept by mutableStateOf<List<KeptDigest>>(emptyList()); private set

    init {
        kept = try {
            val a = JSONArray(Prefs.getString(KEY) ?: "[]")
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val ids = o.optJSONArray("ids") ?: JSONArray()
                KeptDigest(
                    at = o.optLong("at"),
                    minutes = o.optInt("minutes"),
                    ids = (0 until ids.length()).map { ids.getString(it) }.toSet(),
                    text = o.optString("text"),
                    loadMs = o.optLong("loadMs"),
                    aiMs = o.optLong("aiMs"),
                    sources = o.optJSONArray("sources")?.let { a ->
                        (0 until a.length()).mapNotNull { j ->
                            val x = a.optJSONObject(j) ?: return@mapNotNull null
                            DigestSource(
                                x.optInt("ref"),
                                x.optString("id"),
                                x.optString("title"),
                                if (x.has("published")) x.optLong("published") else null,
                            )
                        }
                    } ?: emptyList(),
                )
            }.filter { now() - it.at < DIGEST_KEEP_MS && it.text.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save() {
        val a = JSONArray()
        for (d in kept) {
            a.put(
                JSONObject()
                    .put("at", d.at)
                    .put("minutes", d.minutes)
                    .put("ids", JSONArray(d.ids.toList()))
                    .put("text", d.text)
                    .put("loadMs", d.loadMs)
                    .put("aiMs", d.aiMs)
                    .put(
                        "sources",
                        JSONArray().apply {
                            for (src in d.sources) {
                                put(
                                    JSONObject().put("ref", src.ref).put("id", src.id).put("title", src.title)
                                        .apply { src.published?.let { put("published", it) } },
                                )
                            }
                        },
                    ),
            )
        }
        Prefs.setString(KEY, a.toString())
    }

    /** Scoops a digest from the last day already covers. */
    fun covered(): Set<String> {
        val t = now()
        return kept.filter { t - it.at < DIGEST_KEEP_MS }.flatMap { it.ids }.toSet()
    }

    /** Forget the kept digests, so the next run summarizes the whole window. */
    fun startOver() {
        kept = emptyList()
        upToDate = false
        save()
    }

    /**
     * Claim the engine for a run (false if one is already running). Separate
     * from [run] so a tap shows as running at once, before any coroutine starts.
     */
    fun tryStart(): Boolean {
        if (running) return false
        running = true
        error = null
        upToDate = false
        return true
    }

    /**
     * Make a digest of the last [window] minutes, after [tryStart]. [feed]
     * brings the feed up to date (shown as syncing) and returns it. Returns
     * the new digest, or null when there was nothing new or it failed.
     */
    suspend fun run(window: Int, l: Strings, feed: suspend () -> List<Scoop>): KeptDigest? {
        try {
            syncing = true
            val list = try {
                feed()
            } finally {
                syncing = false
            }
            val done0 = covered()
            val items = inWindow(list, window, now()).filter { it.id !in done0 }
            if (items.isEmpty() && !needsOlder(list, window, now())) {
                upToDate = true
                return null
            }
            return digest(list, items, done0, window, l)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = if (e is SummaryException) e.message else (e.message ?: e.toString())
            error = msg.ifEmpty { l.aiErrorEmpty }
            return null
        } finally {
            syncing = false
            summarizing = false
            progress = null
            running = false
        }
    }

    private suspend fun digest(
        feed: List<Scoop>,
        items: List<Scoop>,
        done0: Set<String>,
        window: Int,
        l: Strings,
    ): KeptDigest? = coroutineScope {
        progress = 0 to items.size
        val started = System.nanoTime()
        val key = AIStore.apiKey() ?: throw SummaryException(l.aiErrorNotConfigured)
        val since = now() - window * 60_000L
        val clock = SimpleDateFormat("HH:mm", Locale.ROOT)
        // Each scoop's main post. Pages are fetched like the list's metadata
        // (20 at a time); a page that fails keeps its headline.
        val gate = Semaphore(20)
        var done = 0
        val ids = items.map { it.id }.toMutableSet()
        val digest = items.map { s ->
            async {
                val body = gate.withPermit {
                    try {
                        RotterService.fetchThread(s.id).root?.bodyHtml
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
                done++
                progress = done to items.size
                DigestItem(clock.format(Date(s.published ?: 0L)), s.title, body, s.id, s.published)
            }
        }.awaitAll().toMutableList()
        // The RSS feed holds only the latest ~74 scoops (a few hours). Scoop
        // thread numbers are consecutive, so walk down from the oldest one in
        // the feed, reading each post's own time, until a batch is older than
        // the window. Already-digested scoops are skipped without fetching.
        if (needsOlder(feed, window, now())) {
            var next = feed.mapNotNull { it.id.toLongOrNull() }.minOrNull()?.minus(1) ?: 0L
            var walked = 0
            while (next > 0 && walked < MAX_OLDER) {
                val batch = (0 until 20).map { next - it }.filter { it > 0 }
                next -= 20
                walked += batch.size
                progress = done to done + 20
                val fresh = batch.filter { it.toString() !in done0 }
                val roots = fresh.map { id ->
                    async {
                        try {
                            RotterService.fetchThread(id.toString()).root?.let { id to it }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            null // removed (404) or a blip: skip it
                        }
                    }
                }.awaitAll().filterNotNull()
                done += batch.size
                val inside = roots.filter { (it.second.timestamp ?: 0L) >= since }
                for ((id, r) in inside) {
                    ids.add(id.toString())
                    digest.add(DigestItem(clock.format(Date(r.timestamp!!)), r.title ?: "", r.bodyHtml, id.toString(), r.timestamp))
                }
                if (roots.isNotEmpty() && inside.isEmpty()) break
            }
        }
        if (digest.isEmpty()) {
            upToDate = true
            return@coroutineScope null
        }
        summarizing = true
        val loaded = System.nanoTime()
        val text = ThreadSummarizer(
            apiKey = key,
            model = AIStore.model,
            baseUrl = AIStore.baseUrl,
            language = AIStore.promptLanguage(SettingsController.locale),
        ).summarizeDigest(digest, "$window minutes")
        val t = now()
        val loadMs = (loaded - started) / 1_000_000
        val aiMs = (System.nanoTime() - loaded) / 1_000_000
        val sources = digest.mapIndexed { i, it -> DigestSource(i + 1, it.id, it.title, it.published) }
        val made = KeptDigest(t, window, ids, text, loadMs, aiMs, sources)
        kept = (listOf(made) + kept).filter { t - it.at < DIGEST_KEEP_MS }
        save()
        made
    }
}
