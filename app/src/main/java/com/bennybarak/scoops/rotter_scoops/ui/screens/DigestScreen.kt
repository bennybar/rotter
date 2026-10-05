package com.bennybarak.scoops.rotter_scoops.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.DigestItem
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.net.SummaryException
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.bennybarak.scoops.rotter_scoops.ui.AISettingsRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalLanguage
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.relTime
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import com.bennybarak.scoops.rotter_scoops.ui.widgets.shareText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** At most this many thread pages are read beyond the feed (≈ a day and a half of scoops). */
private const val MAX_OLDER = 400

/** The time ranges the slider steps through, in minutes: 5 minutes to 24 hours. */
val DIGEST_STOPS = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 960, 1200, 1440)

/**
 * The digest tab: an AI summary of the scoops POSTED (not last commented on)
 * in a chosen window, from their main posts only — no comments are fetched
 * into it. Lives for the session with the home screen.
 */
class DigestState(private val scope: CoroutineScope) {
    var stop by mutableIntStateOf(DIGEST_STOPS.indexOf(60))
    val minutes get() = DIGEST_STOPS[stop]

    var running by mutableStateOf(false); private set
    var progress by mutableStateOf<Pair<Int, Int>?>(null); private set // posts loaded / total
    var summarizing by mutableStateOf(false); private set
    var result by mutableStateOf<String?>(null); private set
    var resultMinutes by mutableIntStateOf(60); private set
    var error by mutableStateOf<String?>(null); private set
    private var job: Job? = null

    /** The scoops posted inside the window, newest first — by posting time. */
    fun inWindow(feed: List<Scoop>, minutes: Int, now: Long = System.currentTimeMillis()): List<Scoop> {
        val since = now - minutes * 60_000L
        return feed.filter { (it.published ?: 0L) >= since }.sortedByDescending { it.published }
    }

    /** True when the feed doesn't reach back to the start of the window. */
    fun needsOlder(feed: List<Scoop>, minutes: Int, now: Long = System.currentTimeMillis()): Boolean {
        val oldest = feed.mapNotNull { it.published }.minOrNull() ?: return false
        return oldest > now - minutes * 60_000L
    }

    fun run(feed: List<Scoop>, l: Strings) {
        val items = inWindow(feed, minutes)
        if (running || (items.isEmpty() && !needsOlder(feed, minutes))) return
        val window = minutes
        job?.cancel()
        job = scope.launch {
            running = true
            error = null
            progress = 0 to items.size
            try {
                val key = AIStore.apiKey() ?: throw SummaryException(l.aiErrorNotConfigured)
                val since = System.currentTimeMillis() - window * 60_000L
                val clock = SimpleDateFormat("HH:mm", Locale.ROOT)
                // Each scoop's main post. Pages are fetched like the list's
                // metadata (20 at a time); a page that fails keeps its headline.
                val gate = Semaphore(20)
                var done = 0
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
                        DigestItem(clock.format(Date(s.published ?: 0L)), s.title, body)
                    }
                }.awaitAll().toMutableList()
                // The RSS feed holds only the latest ~74 scoops (a few hours).
                // Scoop thread numbers are consecutive, so walk down from the
                // oldest one in the feed, reading each post's own time, until a
                // whole batch is older than the window.
                if (needsOlder(feed, window)) {
                    var next = feed.mapNotNull { it.id.toLongOrNull() }.minOrNull()?.minus(1) ?: 0L
                    var walked = 0
                    while (next > 0 && walked < MAX_OLDER) {
                        val batch = (0 until 20).map { next - it }.filter { it > 0 }
                        next -= 20
                        walked += batch.size
                        progress = done to done + 20
                        val roots = batch.map { id ->
                            async {
                                try {
                                    RotterService.fetchThread(id.toString()).root
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    null // removed (404) or a blip: skip it
                                }
                            }
                        }.awaitAll().filterNotNull()
                        done += batch.size
                        val inside = roots.filter { (it.timestamp ?: 0L) >= since }
                        for (r in inside) {
                            digest.add(DigestItem(clock.format(Date(r.timestamp!!)), r.title ?: "", r.bodyHtml))
                        }
                        if (roots.isNotEmpty() && inside.isEmpty()) break
                    }
                }
                summarizing = true
                result = ThreadSummarizer(
                    apiKey = key,
                    model = AIStore.model,
                    baseUrl = AIStore.baseUrl,
                    language = AIStore.promptLanguage(SettingsController.locale),
                ).summarizeDigest(digest, "$window minutes")
                resultMinutes = window
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is SummaryException) e.message else (e.message ?: e.toString())
                error = msg.ifEmpty { l.aiErrorEmpty }
            } finally {
                running = false
                summarizing = false
                progress = null
            }
        }
    }
}

@Composable
fun DigestScreen(s: DigestState, feed: List<Scoop>, bottomInset: Dp) {
    val l = strings
    val p = palette
    val lang = LocalLanguage.current
    val nav = LocalNav.current
    val view = LocalView.current
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val inRange = s.inWindow(feed, s.minutes)
    val oldest = feed.mapNotNull { it.published }.minOrNull()

    AppScaffold(
        bar = {
            AppBar(
                title = { BarTitle(l.digestTitle) },
                actions = {
                    val text = s.result
                    if (text != null && !s.running) {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(null, text))
                            Snacks.show(l.copied, 1000)
                        }) { Icon(Icons.Rounded.ContentCopy, l.copy) }
                        IconButton(onClick = { shareText(context, text) }) { Icon(Icons.Rounded.IosShare, l.share) }
                    }
                },
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp + bottomInset),
        ) {
            item {
                Text(l.digestIntro, style = TextStyle(color = p.muted, lineHeight = 1.45.em, fontSize = 14.sp))
                Spacer(Modifier.height(16.dp))
                // The window and what's in it.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(p.surface, RoundedCornerShape(24.dp))
                        .padding(18.dp),
                ) {
                    Text(l.digestWindow, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700, color = p.muted))
                    Text(
                        l.durationLabel(s.minutes),
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W800, color = p.ink),
                    )
                    Slider(
                        value = s.stop.toFloat(),
                        onValueChange = { v ->
                            val i = v.toInt().coerceIn(0, DIGEST_STOPS.lastIndex)
                            if (i != s.stop) {
                                view.selectionClick()
                                s.stop = i
                            }
                        },
                        valueRange = 0f..DIGEST_STOPS.lastIndex.toFloat(),
                        steps = DIGEST_STOPS.size - 2,
                        enabled = !s.running,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    )
                    val older = s.needsOlder(feed, s.minutes)
                    Text(
                        when {
                            older -> l.scoopsCount(inRange.size) + "+"
                            inRange.isEmpty() -> l.digestNone
                            else -> l.scoopsCount(inRange.size)
                        },
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                            color = if (inRange.isEmpty() && !older) p.muted else accent,
                        ),
                    )
                    // The feed reaches back only a few hours; older scoops in
                    // the window are read from their pages when summarizing.
                    if (older && oldest != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${l.digestFeedReach} ${relTime(oldest, l, lang)} · ${l.digestOlderLoaded}",
                            style = TextStyle(fontSize = 12.5.sp, color = p.muted, lineHeight = 1.4.em),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!AIStore.isReady) {
                        Text(l.digestNeedsKey, style = TextStyle(fontSize = 13.5.sp, color = p.muted, lineHeight = 1.4.em))
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(onClick = { nav.push(AISettingsRoute()) }) { Text(l.aiSection) }
                    } else {
                        Button(
                            onClick = { s.run(feed, l) },
                            enabled = (inRange.isNotEmpty() || s.needsOlder(feed, s.minutes)) && !s.running,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(l.digestButton, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            when {
                s.running -> item {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(14.dp))
                        val pr = s.progress
                        Text(
                            if (s.summarizing || pr == null) l.aiSummarizing else l.digestLoading(pr.first, pr.second),
                            style = TextStyle(color = p.muted),
                        )
                    }
                }
                s.error != null -> item {
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = p.muted, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(l.aiErrorTitle, style = TextStyle(fontWeight = FontWeight.W800, color = p.ink))
                        Spacer(Modifier.height(4.dp))
                        Text(s.error ?: "", style = TextStyle(color = p.muted, textAlign = TextAlign.Center))
                    }
                }
                s.result != null -> {
                    val blocks = summaryBlocks(s.result!!)
                    item {
                        Text(
                            "${l.digestPostedIn} ${l.durationLabel(s.resultMinutes)}",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700, color = p.muted),
                        )
                    }
                    items(blocks.size) { i -> SelectionContainer { SummaryBlockView(blocks[i]) } }
                    item { AiDisclaimer() }
                }
            }
        }
    }
}
