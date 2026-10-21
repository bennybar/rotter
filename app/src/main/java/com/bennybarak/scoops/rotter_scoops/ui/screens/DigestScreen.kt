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
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bennybarak.scoops.rotter_scoops.digest.DigestScheduler
import androidx.compose.material.icons.rounded.Check
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bennybarak.scoops.rotter_scoops.ui.ThreadRoute
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
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
import org.json.JSONObject
import org.json.JSONArray
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.digest.needsOlder as feedNeedsOlder
import com.bennybarak.scoops.rotter_scoops.digest.inWindow as scoopsInWindow
import com.bennybarak.scoops.rotter_scoops.digest.KeptDigest
import com.bennybarak.scoops.rotter_scoops.digest.DigestEngine
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.DigestItem
import com.bennybarak.scoops.rotter_scoops.net.DigestSource
import com.bennybarak.scoops.rotter_scoops.net.splitRefs
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.net.SummaryException
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.bennybarak.scoops.rotter_scoops.ui.AISettingsRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import com.bennybarak.scoops.rotter_scoops.ui.widgets.shareText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The time ranges to choose from, in minutes: 15 minutes to 24 hours. */
val DIGEST_STOPS = listOf(15, 30, 60, 180, 480, 1440)

/**
 * The Digest screen's state: the window, "new only", and a view of the
 * [DigestEngine] that makes the digests. In the app a run goes to the
 * background worker ([runner]); without one (tests) it runs in [scope].
 */
class DigestState(
    private val scope: CoroutineScope,
    val engine: DigestEngine = DigestEngine(),
    private val runner: ((window: Int) -> Unit)? = null,
    private val now: () -> Long = engine.now,
) {
    constructor(scope: CoroutineScope, now: () -> Long) : this(scope, DigestEngine(now), null, now)

    companion object {
        private const val NEW_ONLY_KEY = "digest_new_only"
    }

    var stop by mutableIntStateOf(DIGEST_STOPS.indexOf(60))
    val minutes get() = DIGEST_STOPS[stop]

    /**
     * Show only the newest digest run (what's new since the previous digest),
     * not the earlier ones still kept. Remembered until changed.
     */
    var newOnly by mutableStateOf(Prefs.getBool(NEW_ONLY_KEY) ?: false); private set

    fun setNewOnlyAndSave(v: Boolean) {
        newOnly = v
        Prefs.setBool(NEW_ONLY_KEY, v)
    }

    val kept: List<KeptDigest> get() = engine.kept
    val running: Boolean get() = engine.running
    val syncing: Boolean get() = engine.syncing
    val progress: Pair<Int, Int>? get() = engine.progress
    val summarizing: Boolean get() = engine.summarizing
    val error: String? get() = engine.error
    val upToDate: Boolean get() = engine.upToDate
    val counting: Boolean get() = engine.counting

    /** The kept digests the screen considers: all of them, or just the newest. */
    fun considered(): List<KeptDigest> = if (newOnly) kept.take(1) else kept
    fun covered() = engine.covered()
    fun startOver() = engine.startOver()
    fun inWindow(feed: List<Scoop>, minutes: Int, now: Long = this.now()): List<Scoop> = scoopsInWindow(feed, minutes, now)
    fun needsOlder(feed: List<Scoop>, minutes: Int, now: Long = this.now()): Boolean = feedNeedsOlder(feed, minutes, now)

    /**
     * Start a digest of the selected window. It responds at once; in the app
     * it runs in the background worker (with its notification), which brings
     * the feed up to date itself. In tests: a feed older than a minute is
     * caught up first, for at most 5 seconds.
     */
    fun run(feedNow: () -> List<Scoop>, catchUp: suspend () -> Unit, feedFresh: Boolean, l: Strings) {
        val window = minutes
        runner?.let {
            if (!engine.running) it(window)
            return
        }
        if (!engine.tryStart()) return
        scope.launch {
            engine.run(window, l) {
                if (!feedFresh) withTimeoutOrNull(5_000) { catchUp() }
                feedNow()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigestScreen(
    s: DigestState,
    feed: List<Scoop>,
    feedNow: () -> List<Scoop>,
    feedLoading: Boolean,
    feedFresh: () -> Boolean,
    catchUp: suspend () -> Unit,
    bottomInset: Dp,
) {
    val ui = rememberCoroutineScope()
    // Catch up whenever the page is shown — switching to the tab or coming
    // back to the app on it — so its window includes the latest scoops.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ui.launch { catchUp() } }
    var refreshing by remember { mutableStateOf(false) }
    // Android 13+: the digest job's notifications need permission; ask the
    // first time a digest is started or the schedule is turned on.
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ctx = LocalContext.current
    fun askToNotify() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var auto by remember { mutableIntStateOf(DigestScheduler.autoMinutes) }
    var menu by remember { mutableStateOf(false) }
    val nextAuto by remember { DigestScheduler.nextAuto(ctx) }.collectAsState(null)
    val l = strings
    val p = palette
    val nav = LocalNav.current
    val view = LocalView.current
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val clock = remember { SimpleDateFormat("HH:mm", Locale.ROOT) }
    val inRange = s.inWindow(feed, s.minutes)
    val covered = s.covered()
    val older = s.needsOlder(feed, s.minutes)
    // The feed holds only the latest ~74 scoops: a longer window is counted
    // by reading the posting times of the scoops below it, in the background.
    val oldestId = feed.mapNotNull { it.id.toLongOrNull() }.minOrNull()
    LaunchedEffect(s.minutes, oldestId) { if (older) s.engine.countOlder(feed, s.minutes) }
    val below = if (older) s.engine.olderIn(s.minutes).keys - inRange.map { it.id }.toSet() else emptySet()
    val total = inRange.size + below.size
    // Still counting, or the walk stopped short of the window's start.
    val more = older && (s.counting || s.engine.olderCapped)
    val since = System.currentTimeMillis() - s.minutes * 60_000L
    // Kept digests narrowed to the selected window; empty ones drop out.
    val shown = s.considered().map { it to visibleBlocks(it, since) }.filter { it.second.isNotEmpty() }
    val fresh = inRange.count { it.id !in covered } + below.count { it !in covered }

    AppScaffold(
        bar = {
            AppBar(
                title = { BarTitle(l.digestTitle) },
                actions = {
                    val text = shown.takeIf { it.isNotEmpty() }?.joinToString("\n\n") { blocksToText(it.second) }
                    if (text != null && !s.running) {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(null, text))
                            Snacks.show(l.copied, 1000)
                        }) { Icon(Icons.Rounded.ContentCopy, l.copy) }
                        IconButton(onClick = { shareText(context, text) }) { Icon(Icons.Rounded.IosShare, l.share) }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, l.moreOptions) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            // New only: just the latest run, not earlier digests.
                            DropdownMenuItem(
                                text = { Text(l.digestNewOnly, style = TextStyle(fontSize = 15.sp)) },
                                leadingIcon = {
                                    if (s.newOnly) Icon(Icons.Rounded.Check, null) else Spacer(Modifier.size(24.dp))
                                },
                                onClick = {
                                    menu = false
                                    s.setNewOnlyAndSave(!s.newOnly)
                                },
                            )
                            HorizontalDivider()
                            // Automatic digests on a schedule, in the background.
                            Text(
                                l.digestAuto,
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700, color = p.muted),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            for (m in DigestScheduler.AUTO_CHOICES) {
                                DropdownMenuItem(
                                    text = { Text(l.digestAutoChoice(m), style = TextStyle(fontSize = 15.sp)) },
                                    leadingIcon = {
                                        if (m == auto) Icon(Icons.Rounded.Check, null) else Spacer(Modifier.size(24.dp))
                                    },
                                    enabled = AIStore.isReady,
                                    onClick = {
                                        menu = false
                                        if (m > 0) askToNotify()
                                        auto = m
                                        DigestScheduler.setAuto(ctx, m)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                ui.launch {
                    refreshing = true
                    try {
                        catchUp()
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp + bottomInset),
        ) {
            item {
                // The window: a few fixed ranges, one tap each.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DIGEST_STOPS.forEachIndexed { i, m ->
                        val on = i == s.stop
                        Box(
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) accent else p.surface)
                                .clickable(enabled = !s.running && !on) {
                                    view.selectionClick()
                                    s.stop = i
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                l.digestChip(m),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = if (on) FontWeight.W800 else FontWeight.W600,
                                    color = if (on) onAccent else p.ink,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // What's waiting to be summarized, and the button that does it.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    when {
                        !AIStore.isReady -> Column {
                            Text(l.digestNeedsKey, style = TextStyle(fontSize = 14.sp, color = p.ink, lineHeight = 1.4.em))
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(onClick = { nav.push(AISettingsRoute()) }) { Text(l.aiSection) }
                        }
                        s.running -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.width(14.dp))
                            val pr = s.progress
                            Text(
                                when {
                                    s.syncing -> l.digestUpdating
                                    s.summarizing || pr == null -> l.aiSummarizing
                                    else -> l.digestLoading(pr.first, pr.second)
                                },
                                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W700, color = p.ink),
                            )
                        }
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(if (fresh > 0) accent else accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    s.error != null -> Icon(Icons.Rounded.ErrorOutline, null, tint = accent)
                                    fresh > 0 -> Text(
                                        "$fresh",
                                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W800, color = onAccent),
                                    )
                                    else -> Icon(Icons.Rounded.Check, null, tint = accent)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when {
                                        s.error != null -> l.aiErrorTitle
                                        fresh > 0 -> l.digestNewSince(fresh, s.kept.firstOrNull()?.let { clock.format(Date(it.at)) })
                                        total == 0 && !more -> l.digestNone
                                        else -> l.digestUpToDate
                                    },
                                    style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.W700, color = p.ink, lineHeight = 1.3.em),
                                    maxLines = 2,
                                )
                                val sub = listOfNotNull(
                                    when {
                                        feedLoading -> l.digestUpdating
                                        s.counting -> l.digestCounting
                                        else -> l.scoopsCount(total) + (if (more) "+" else "") + " · " + l.durationLabel(s.minutes)
                                    },
                                    nextAuto?.let { l.digestNextAuto(clock.format(Date(it))) },
                                )
                                Text(
                                    sub.joinToString(" · "),
                                    style = TextStyle(fontSize = 12.sp, color = p.muted, lineHeight = 1.35.em),
                                )
                                s.error?.let {
                                    Text(it, style = TextStyle(fontSize = 12.sp, color = p.muted, lineHeight = 1.35.em), maxLines = 3)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                // Responds at once; a stale feed is synced inside the run.
                                onClick = {
                                    askToNotify()
                                    s.run(feedNow, catchUp, feedFresh(), l)
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.height(40.dp),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(l.digestButton, style = TextStyle(fontWeight = FontWeight.W700))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            // The kept digests, newest first; each covers only scoops the
            // ones before it hadn't. Only what the selected window covers.
            shown.forEachIndexed { i, (d, blocks) ->
                item {
                    val inWindow = if (d.sources.isEmpty()) d.ids.size else d.sources.count { (it.published ?: 0L) >= since }
                    Text(
                        l.digestMadeAt(clock.format(Date(d.at)), inWindow),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700, color = if (i == 0) accent else p.muted),
                        modifier = Modifier.padding(start = 4.dp, top = if (i > 0) 14.dp else 0.dp, bottom = 8.dp),
                    )
                }
                // One card per topic: its heading and the lines under it.
                val cards = ArrayList<Pair<SummaryBlock?, MutableList<SummaryBlock>>>()
                for (b in blocks) {
                    if (b.kind == SummaryBlockKind.heading) cards.add(b to mutableListOf())
                    else if (cards.isEmpty()) cards.add(null to mutableListOf(b))
                    else cards.last().second.add(b)
                }
                items(cards.size) { j ->
                    val (heading, lines) = cards[j]
                    Column(
                        Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .background(p.surface, RoundedCornerShape(26.dp))
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                    ) {
                        if (heading != null) {
                            Directional(heading.text) {
                                Text(
                                    inline(splitRefs(heading.text).first),
                                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.W800, color = p.ink),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                )
                            }
                        }
                        lines.forEachIndexed { k, b ->
                            if (k > 0 || heading != null) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(p.ink.copy(alpha = 0.07f)))
                            }
                            DigestLine(b, d.sources)
                        }
                    }
                }
                // How long it took, at its bottom (older kept digests have no timing).
                if (d.loadMs + d.aiMs > 0) {
                    item {
                        Text(
                            l.digestRuntime(d.loadMs + d.aiMs, d.loadMs, d.aiMs),
                            style = TextStyle(fontSize = 12.sp, color = p.muted),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
            if (shown.isNotEmpty() && !s.running) {
                item {
                    AiDisclaimer()
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { s.startOver() }) { Text(l.digestStartOver) }
                }
            }
        }
        }
    }
}

/** The posting time a digest line starts with ("11:08 …", possibly in bold). */
private val leadingTimeRe = Regex("""^(?:\*\*\s*(\d{1,2}:\d{2})\s*\*\*|(\d{1,2}:\d{2}))\s*[-–—:·|]?\s*""")

/**
 * A digest line: its posting time in a column of its own, then the text. Its
 * scoop references are hidden; tapping the line opens the thread it's based
 * on, or — when it merges several — a menu of them.
 */
@Composable
private fun DigestLine(b: SummaryBlock, sources: List<DigestSource>) {
    val nav = LocalNav.current
    val view = LocalView.current
    val p = palette
    val accent = MaterialTheme.colorScheme.primary
    val (text, refs) = remember(b.text) { splitRefs(b.text) }
    val time = remember(text) { leadingTimeRe.find(text) }
    val body = if (time != null) text.substring(time.range.last + 1) else text
    val linked = refs.mapNotNull { r -> sources.firstOrNull { it.ref == r && it.id.isNotEmpty() } }
    val line = @Composable {
        Directional(body) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                when {
                    time != null -> Text(
                        time.groupValues[1].ifEmpty { time.groupValues[2] },
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700, color = accent),
                        modifier = Modifier.width(46.dp).padding(top = 2.dp),
                    )
                    b.kind == SummaryBlockKind.item -> Text(b.marker ?: "•", style = TextStyle(fontSize = 15.sp, color = p.muted), modifier = Modifier.width(22.dp))
                }
                Text(
                    inline(body),
                    style = TextStyle(fontSize = 15.5.sp, lineHeight = 1.45.em, color = p.ink),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    if (linked.isEmpty()) {
        SelectionContainer { line() }
        return
    }
    fun open(src: DigestSource) {
        view.selectionClick()
        nav.push(ThreadRoute(Scoop(src.id, src.title, RotterService.threadUrl(src.id), src.published)))
    }
    var menu by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { if (linked.size == 1) open(linked.first()) else menu = true },
    ) {
        line()
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val clock = SimpleDateFormat("HH:mm", Locale.ROOT)
            for (src in linked) {
                DropdownMenuItem(
                    text = {
                        Text(
                            (src.published?.let { clock.format(Date(it)) + " · " } ?: "") + src.title,
                            maxLines = 2,
                            style = TextStyle(fontSize = 14.sp),
                        )
                    },
                    onClick = {
                        menu = false
                        open(src)
                    },
                )
            }
        }
    }
}

/**
 * The lines of [d] about scoops posted since [since] — what a narrower window
 * than the digest was made for should show. A line counts by the posting
 * times of the scoops it references; untagged lines (and digests from before
 * references) count by when the digest was made. Headings stay only when a
 * line under them does.
 */
fun visibleBlocks(d: KeptDigest, since: Long): List<SummaryBlock> {
    val blocks = summaryBlocks(d.text)
    val madeInside = d.at >= since
    if (d.sources.isEmpty()) return if (madeInside) blocks else emptyList()
    fun inside(b: SummaryBlock): Boolean {
        val refs = splitRefs(b.text).second
        val times = refs.mapNotNull { r -> d.sources.firstOrNull { it.ref == r }?.published }
        return if (times.isEmpty()) madeInside else times.any { it >= since }
    }
    val out = ArrayList<SummaryBlock>()
    var heading: SummaryBlock? = null
    for (b in blocks) {
        when (b.kind) {
            SummaryBlockKind.heading -> heading = b
            else -> if (inside(b)) {
                heading?.let { out.add(it) }
                heading = null
                out.add(b)
            }
        }
    }
    return out
}

/** Blocks back to Markdown, without the scoop references (for copy / share). */
fun blocksToText(blocks: List<SummaryBlock>): String = blocks.joinToString("\n") { b ->
    val t = splitRefs(b.text).first
    when (b.kind) {
        SummaryBlockKind.heading -> "\n## $t"
        SummaryBlockKind.item -> "${b.marker ?: "-"} $t"
        SummaryBlockKind.paragraph -> t
    }
}.trim()
