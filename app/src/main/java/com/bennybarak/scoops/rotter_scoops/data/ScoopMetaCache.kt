package com.bennybarak.scoops.rotter_scoops.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bennybarak.scoops.rotter_scoops.net.CardMeta
import com.bennybarak.scoops.rotter_scoops.net.RotterHttpException
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

data class ScoopMeta(
    val author: String? = null,
    val authorPoints: Int? = null, // the poster's reputation, for the chip by their name
    val replies: Int? = null,
    val lastComment: Long? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (author != null) put("author", author)
        if (authorPoints != null) put("authorPoints", authorPoints)
        if (replies != null) put("replies", replies)
        if (lastComment != null) put("lastComment", lastComment)
    }

    companion object {
        fun fromJson(j: JSONObject) = ScoopMeta(
            author = if (j.isNull("author")) null else j.getString("author"),
            authorPoints = if (j.isNull("authorPoints")) null else j.getInt("authorPoints"),
            replies = if (j.isNull("replies")) null else j.getInt("replies"),
            lastComment = if (j.isNull("lastComment")) null else j.getLong("lastComment"),
        )
    }
}

/**
 * Fetches and caches per-card metadata (poster, their points, reply count,
 * last-comment time) — none of which is in the RSS.
 *
 * - **Bounded priority queue**: work waits its turn instead of being dropped
 *   when the cap is hit; cards on screen jump to the front ([ensure]), the
 *   rest of the feed is warmed behind them ([prefetch]) — needed for "sort by
 *   last comment" to mean anything.
 * - **Persisted** to the cache dir, so a relaunch paints complete cards
 *   instantly. Entries older than [FRESH_FOR_MS] are still *shown* and
 *   refreshed underneath; [revalidate] (pull-to-refresh) marks everything stale
 *   without blanking it, so only threads that actually changed move.
 * - **404 vs failure** are kept apart: a 404 is "the thread is gone" and not
 *   retried; a network blip keeps whatever was shown and retries after a
 *   cooldown, instead of leaving a permanent skeleton.
 * - Every network arrival feeds [ReadStore.reconcile], so read threads notice
 *   new comments from this fetch rather than a second pass over the same pages.
 *
 * Main-thread only (the fetch itself runs on IO/Default inside [fetch]).
 */
class ScoopMetaCache(
    private val fetch: suspend (String) -> CardMeta = RotterService::fetchCardMeta,
    private val persists: Boolean = true,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val now: () -> Long = System::currentTimeMillis,
) {
    companion object {
        val instance by lazy { ScoopMetaCache() }

        /**
         * Measured on iOS: filling the whole feed takes 6.9s at 6, 2.6s at 20 —
         * the work is latency-bound. 20 was checked against the live site.
         */
        const val MAX_IN_FLIGHT = 20
        private const val FILE_NAME = "scoop-meta.json"
        private const val FRESH_FOR_MS = 10 * 60_000L
        private const val MAX_PERSISTED = 400
        private const val RETRY_COOLDOWN_MS = 30_000L
    }

    private val loaded = HashMap<String, ScoopMeta>()
    private val unavailable = HashSet<String>()
    private val fetchedAt = HashMap<String, Long>()
    private val failedAt = HashMap<String, Long>()
    private val inFlight = HashSet<String>()
    private val queue = ArrayList<String>() // nearest-to-the-viewport first
    private val stale = HashSet<String>()
    private var batchTotal = 0
    private var batchDone = 0
    private var saveJob: Job? = null

    /**
     * Per-id redraw signal: bumped only when that card's display actually
     * changed, so an arrival redraws its own card and nothing else.
     */
    private val ticks = mutableStateMapOf<String, Int>()

    /** Backfill progress 0..1 while card fetches are running, else null. */
    var progress by mutableStateOf<Float?>(null); private set

    /**
     * Last-comment times the list sorts by, refreshed at most every 600ms: read
     * live, every one of ~75 arrivals would re-sort the whole list.
     */
    var orderingTimes by mutableStateOf<Map<String, Long>>(emptyMap()); private set
    private var orderingJob: Job? = null
    private var orderingPaused = false

    /**
     * Seed from disk. NOT fed to [ReadStore.reconcile]: those counts are as old
     * as the file, and would flag comments as new from last session's numbers.
     */
    fun load() {
        if (!persists) return
        val raw = DiskCache.readNow(FILE_NAME) as? JSONObject ?: return
        try {
            for (id in raw.keys()) {
                val e = raw.getJSONObject(id)
                loaded[id] = ScoopMeta.fromJson(e.getJSONObject("meta"))
                fetchedAt[id] = e.getLong("fetchedAt")
            }
        } catch (_: Exception) { /* corrupt → refetch */ }
        orderingTimes = currentTimes()
    }

    /** Subscribe the calling composable to [id]'s changes. */
    fun observe(id: String) {
        ticks[id]
    }

    fun of(id: String): ScoopMeta? = loaded[id]

    /** The thread 404s. Distinct from a failed request. */
    fun isUnavailable(id: String) = unavailable.contains(id)

    /** Still waiting on a first answer — the only state that shows a placeholder. */
    fun isPending(id: String) = inFlight.contains(id) ||
        (!loaded.containsKey(id) && !unavailable.contains(id) && !failedAt.containsKey(id))

    /**
     * Keep rows in place while the reader is scrolling; card details still
     * update. An explicit refresh or sort change releases it.
     */
    fun setOrderingPaused(paused: Boolean) {
        orderingPaused = paused
        if (!paused) scheduleOrdering()
    }

    /**
     * Mark everything for re-fetch WITHOUT discarding what's on screen (pull to
     * refresh). The queue is left alone so cards already waiting aren't lost.
     */
    fun revalidate() {
        stale.addAll(loaded.keys)
    }

    /** A card is on screen: move it to the head of the queue. */
    fun ensure(id: String) {
        if (!shouldFetch(id)) return
        if (queue.remove(id)) {
            queue.add(0, id)
            return
        }
        queue.add(0, id)
        batchTotal++
        pump()
    }

    /** Warm these in the background, behind anything already on screen. */
    fun prefetch(ids: Iterable<String>) {
        for (id in ids) {
            if (shouldFetch(id) && !queue.contains(id)) {
                queue.add(id)
                batchTotal++
            }
        }
        pump()
    }

    private fun shouldFetch(id: String): Boolean {
        if (inFlight.contains(id)) return false
        // Back off after a failure whatever else we know, so a host that is
        // down isn't retried once per visible row.
        val failed = failedAt[id]
        if (failed != null && now() - failed <= RETRY_COOLDOWN_MS) return false
        if (stale.contains(id)) return true
        if (unavailable.contains(id)) return false
        if (!loaded.containsKey(id)) return true
        // Judged live, not only at launch — a session left open all afternoon
        // must not keep showing the morning's reply counts.
        val at = fetchedAt[id]
        return at == null || now() - at > FRESH_FOR_MS
    }

    private fun pump() {
        while (inFlight.size < MAX_IN_FLIGHT && queue.isNotEmpty()) {
            val id = queue.removeAt(0)
            if (shouldFetch(id)) start(id)
        }
        publishProgress()
    }

    private fun start(id: String) {
        inFlight.add(id)
        stale.remove(id)
        // Cards redraw only when this id's display actually changes.
        val hadValue = loaded.containsKey(id)
        scope.launch {
            var changed = !hadValue // a placeholder resolves either way
            try {
                val m = fetch(id)
                val meta = ScoopMeta(m.author, m.authorPoints, m.replies, m.lastComment)
                fetchedAt[id] = now()
                failedAt.remove(id)
                if (unavailable.remove(id)) changed = true
                if (loaded[id] != meta) {
                    loaded[id] = meta
                    changed = true
                    scheduleOrdering()
                }
                // Saved either way: the timestamp moved even when the values
                // didn't, and it's what the freshness window reads next launch.
                scheduleSave()
                ReadStore.reconcile(id, m.replies)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is RotterHttpException && e.isNotFound) {
                    if (unavailable.add(id)) changed = true
                    if (loaded.remove(id) != null) {
                        scheduleOrdering()
                        scheduleSave()
                    }
                } else {
                    // Keep whatever was showing — blanking a complete card over
                    // one dropped request is what revalidate() works to avoid.
                    failedAt[id] = now()
                }
            } finally {
                inFlight.remove(id)
                batchDone++
                if (changed) ticks[id] = (ticks[id] ?: 0) + 1
                pump()
            }
        }
    }

    private fun publishProgress() {
        if (queue.isEmpty() && inFlight.isEmpty()) {
            // Forget the batch so the next one starts from empty.
            batchTotal = 0
            batchDone = 0
            progress = null
        } else if (batchTotal > 0) {
            progress = batchDone.toFloat() / batchTotal
        }
    }

    private fun currentTimes(): Map<String, Long> = buildMap {
        for ((id, m) in loaded) m.lastComment?.let { put(id, it) }
    }

    private fun scheduleOrdering() {
        if (orderingPaused || orderingJob?.isActive == true) return
        orderingJob = scope.launch {
            delay(600)
            if (orderingPaused) return@launch // a scroll started while waiting
            val times = currentTimes()
            if (times != orderingTimes) orderingTimes = times
        }
    }

    /** Written a couple of seconds after the last arrival, not per arrival. */
    private fun scheduleSave() {
        if (!persists || saveJob?.isActive == true) return
        saveJob = scope.launch {
            delay(2000)
            // Loaded entries only, newest first, capped. Failures and 404s are
            // judgements about one moment — persisting them makes rows stick blank.
            val ids = loaded.keys.sortedByDescending { fetchedAt[it] ?: 0L }
            val o = JSONObject()
            for (id in ids.take(MAX_PERSISTED)) {
                o.put(
                    id,
                    JSONObject()
                        .put("meta", loaded[id]!!.toJson())
                        .put("fetchedAt", fetchedAt[id] ?: now()),
                )
            }
            DiskCache.write(FILE_NAME, o.toString())
        }
    }
}
