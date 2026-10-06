package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.DiskCache
import com.bennybarak.scoops.rotter_scoops.data.MyRepliesStore
import com.bennybarak.scoops.rotter_scoops.data.ReadStore
import com.bennybarak.scoops.rotter_scoops.data.SavedStore
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.ScoopMetaCache
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.data.SortMode
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import com.bennybarak.scoops.rotter_scoops.ui.LocalChromeDirection
import com.bennybarak.scoops.rotter_scoops.ui.LocalLanguage
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.Mine
import com.bennybarak.scoops.rotter_scoops.ui.ScaledText
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.ThreadRoute
import com.bennybarak.scoops.rotter_scoops.ui.ComposeRoute
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AvatarBubble
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarHeight
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.PointsChip
import com.bennybarak.scoops.rotter_scoops.ui.widgets.ScrollHidingScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.mediumImpact
import com.bennybarak.scoops.rotter_scoops.ui.widgets.openExternal
import com.bennybarak.scoops.rotter_scoops.ui.widgets.relTime
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import com.bennybarak.scoops.rotter_scoops.ui.widgets.shareText
import com.bennybarak.scoops.rotter_scoops.ui.widgets.dropShadowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which scoops the list shows. */
enum class ScoopFilter { all, unread, newComments, mine, saved, following }

private enum class Bucket { today, yesterday, earlier }

/**
 * The scoops list's state and logic. Lives for the app session (the list tab
 * is never torn down), like the Flutter screen kept alive in its IndexedStack.
 */
class ScoopsController(private val scope: CoroutineScope) {
    companion object {
        private const val LIST_FILE = "scoops.json"

        /** Roughly two screens of rows — what post-time order prefetches. */
        private const val FIRST_SCREENFUL = 20
    }

    private val meta = ScoopMetaCache.instance
    val listState = LazyListState()

    var query by mutableStateOf("")
    var searching by mutableStateOf(false) // toggles the in-bar search field
    var filter by mutableStateOf(ScoopFilter.all)

    /**
     * The list on screen. Seeded from disk so a launch paints instantly and the
     * feed request becomes a refresh of something already there.
     */
    var loaded by mutableStateOf<List<Scoop>>(emptyList()); private set

    /**
     * A feed fetched on return from the background, held back behind the "new
     * scoops" pill instead of reshuffling the list under the reader.
     */
    var pending by mutableStateOf<List<Scoop>?>(null); private set

    var didFetch by mutableStateOf(false); private set // a network load completed this session
    var failed by mutableStateOf(false); private set // first load failed (nothing to show)
    var refreshFailed by mutableStateOf(false); private set // a refresh failed while a list was showing
    private var refreshFailedJob: Job? = null
    var feedLoading by mutableStateOf(false); private set
    private var inFlight: Deferred<Unit>? = null
    private var backgroundedAt: Long? = null
    private var started = false

    private val sortListener: () -> Unit = ::onSortChanged

    init {
        SettingsController.addSortListener(sortListener)
    }

    /** The activity is gone: stop listening to the app-wide sort setting. */
    fun dispose() {
        SettingsController.removeSortListener(sortListener)
    }

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val cached = DiskCache.read(LIST_FILE) as? JSONArray
            if (cached != null && cached.length() > 0 && !didFetch) {
                // Disposable data: an entry that doesn't decode (missing field,
                // wrong type) means the whole cache is ignored, not a crash.
                decodeCachedList(cached)?.let { loaded = it }
            }
            load()
            meta.prefetch(SavedStore.followed.scoops.map { it.id })
        }
    }

    fun onPaused() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onResumed() {
        val at = backgroundedAt ?: return
        backgroundedAt = null
        if (System.currentTimeMillis() - at < 2 * 60_000) return
        // Check quietly: keep the rows where they are and offer the new feed.
        meta.setOrderingPaused(true)
        meta.prefetch(SavedStore.followed.scoops.map { it.id })
        if (inFlight == null) {
            val d = scope.async { performLoad(stage = true) }
            inFlight = d
            d.invokeOnCompletion { if (inFlight === d) inFlight = null }
        }
    }

    private fun onSortChanged() {
        meta.setOrderingPaused(false)
        // Last-comment order needs a time for every thread, not just fetched ones.
        if (SettingsController.sortMode == SortMode.lastComment) meta.prefetch(loaded.map { it.id })
    }

    /**
     * Loads the feed, joining a load already in progress rather than racing it
     * (an older response landing last would overwrite a newer one).
     */
    suspend fun load() {
        val existing = inFlight
        if (existing != null) {
            existing.await()
            applyPending()
            // A pull that arrived mid-load has since marked metadata stale.
            prefetchMeta(loaded)
            return
        }
        val d = scope.async { performLoad() }
        inFlight = d
        try {
            d.await()
        } finally {
            if (inFlight === d) inFlight = null
        }
    }

    private suspend fun performLoad(stage: Boolean = false) {
        feedLoading = true
        try {
            val scoops = RotterService.fetchScoopsPreferringLaunch()
            didFetch = true
            if (stage && loaded.isNotEmpty()) {
                refreshFailed = false
                pending = if (scoops == loaded) null else scoops
                return
            }
            pending = null
            loaded = scoops
            failed = false
            refreshFailed = false
            persist(scoops)
            prefetchMeta(scoops)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // A first load that fails shows the error state; a failed REFRESH
            // keeps the list being read, but says so.
            failed = loaded.isEmpty()
            refreshFailed = loaded.isNotEmpty()
            refreshFailedJob?.cancel()
            refreshFailedJob = scope.launch {
                delay(5000)
                refreshFailed = false
            }
        } finally {
            feedLoading = false
        }
    }

    fun applyPending() {
        val p = pending ?: return
        loaded = p
        pending = null
        persist(p)
        meta.revalidate()
        prefetchMeta(p)
    }

    private fun persist(scoops: List<Scoop>) {
        val a = JSONArray()
        for (s in scoops) a.put(s.toJson())
        scope.launch { DiskCache.write(LIST_FILE, a.toString()) }
    }

    /**
     * Last-comment order needs a time for EVERY thread (else an older thread
     * that just got a reply can never rise). Post-time order only needs a
     * screenful. Either way read threads are included: their live reply count
     * is how new comments are noticed.
     */
    private fun prefetchMeta(scoops: List<Scoop>) {
        if (SettingsController.sortMode == SortMode.lastComment) {
            meta.prefetch(scoops.map { it.id })
            return
        }
        val first = scoops.take(FIRST_SCREENFUL).map { it.id }.toSet()
        meta.prefetch(first + scoops.filter { ReadStore.isRead(it.id) && it.id !in first }.map { it.id })
    }

    /**
     * Fetch the latest feed now and show it (no "new scoops" pill): the digest
     * tab's catch-up. Joins a load already in flight.
     */
    fun catchUp() {
        scope.launch {
            load()
            applyPending()
            android.util.Log.d("Scoops", "digest catch-up: ${loaded.size} scoops")
        }
    }

    suspend fun refresh() {
        // Re-fetch card meta even inside its freshness window — a manual pull
        // should always update. Old values stay on screen until replaced.
        meta.setOrderingPaused(false)
        meta.revalidate()
        load()
        meta.prefetch(SavedStore.followed.scoops.map { it.id })
    }

    fun toggleRead(s: Scoop) {
        if (ReadStore.isRead(s.id)) {
            ReadStore.markUnread(s.id)
            return
        }
        // Use the count the card is already showing rather than refetching it.
        val known = meta.of(s.id)?.replies
        if (known != null) {
            ReadStore.markRead(s.id, known)
            return
        }
        // Not fetched yet: mark read now and let the metadata arrival fill in
        // the baseline (reconcile resolves the pending state).
        ReadStore.markRead(s.id, ReadStore.PENDING)
        meta.ensure(s.id)
    }

    fun stopSearch() {
        searching = false
        query = ""
    }

    fun matches(f: ScoopFilter, s: Scoop): Boolean = when (f) {
        ScoopFilter.all -> true
        ScoopFilter.unread -> !ReadStore.isRead(s.id)
        ScoopFilter.newComments -> ReadStore.isNew(s.id)
        ScoopFilter.mine -> MyRepliesStore.replied(s.id)
        ScoopFilter.saved -> SavedStore.saved.contains(s.id)
        ScoopFilter.following -> SavedStore.followed.contains(s.id)
    }

    /**
     * Saved/followed threads may have left the rolling feed, so those filters
     * read from their own stores rather than the feed.
     */
    fun source(f: ScoopFilter): List<Scoop> = when (f) {
        ScoopFilter.saved -> SavedStore.saved.scoops
        ScoopFilter.following -> SavedStore.followed.scoops
        else -> loaded
    }

    fun count(f: ScoopFilter) = source(f).count { matches(f, it) }

    /**
     * The effective time a scoop is sorted/grouped by: post time, or its last
     * comment (falling back to post time until that meta has loaded). Reads the
     * throttled ordering snapshot, not the live cache.
     */
    fun effTime(s: Scoop, mode: SortMode, times: Map<String, Long>): Long {
        val published = s.published ?: 0L
        if (mode == SortMode.postTime) return published
        val last = times[s.id]
        return if (last != null && last > published) last else published
    }
}

/** The cached feed, or null when any entry fails to decode. */
internal fun decodeCachedList(a: JSONArray): List<Scoop>? = try {
    (0 until a.length()).map { Scoop.fromJson(a.getJSONObject(it)) }
} catch (_: org.json.JSONException) {
    null
}

private fun bucketOf(d: Long): Bucket {
    if (d == 0L) return Bucket.earlier
    fun dayStart(ms: Long) = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val today = dayStart(System.currentTimeMillis())
    val day = dayStart(d)
    // Whole calendar days between them (DST-safe: compare the dates, not millis).
    var diff = 0
    val c = day.clone() as Calendar
    if (!c.before(today)) return Bucket.today
    while (c.before(today) && diff < 2) {
        c.add(Calendar.DAY_OF_MONTH, 1)
        diff++
    }
    return if (diff == 1) Bucket.yesterday else Bucket.earlier
}

private fun bucketLabel(b: Bucket, l: Strings) = when (b) {
    Bucket.today -> l.today
    Bucket.yesterday -> l.yesterday
    Bucket.earlier -> l.earlier
}

private fun filterLabel(f: ScoopFilter, l: Strings) = when (f) {
    ScoopFilter.all -> l.filterAll
    ScoopFilter.unread -> l.filterUnread
    ScoopFilter.newComments -> l.filterNewComments
    ScoopFilter.mine -> l.filterMine
    ScoopFilter.saved -> l.filterSaved
    ScoopFilter.following -> l.filterFollowing
}

private fun filterIcon(f: ScoopFilter): ImageVector = when (f) {
    ScoopFilter.all -> Icons.Rounded.Inbox
    ScoopFilter.unread -> Icons.Rounded.Circle
    ScoopFilter.newComments -> Icons.Rounded.AutoAwesome
    ScoopFilter.mine -> Icons.Outlined.Forum
    ScoopFilter.saved -> Icons.Rounded.BookmarkBorder
    ScoopFilter.following -> Icons.Rounded.NotificationsNone
}

private fun emptyText(f: ScoopFilter, l: Strings) = when (f) {
    ScoopFilter.all -> l.emptyScoops
    ScoopFilter.unread -> l.emptyUnread
    ScoopFilter.newComments -> l.emptyNewComments
    ScoopFilter.mine -> l.noMyReplies
    ScoopFilter.saved -> l.emptySaved
    ScoopFilter.following -> l.emptyFollowing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoopsScreen(c: ScoopsController, bottomInset: androidx.compose.ui.unit.Dp) {
    val l = strings
    val p = palette
    val nav = LocalNav.current
    val view = LocalView.current
    val context = LocalContext.current
    val meta = ScoopMetaCache.instance
    val uiScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { c.start() }

    // Keep rows in place once the reader starts dragging, so a metadata
    // arrival can't move the row under their thumb.
    LaunchedEffect(c.listState) {
        c.listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) meta.setOrderingPaused(true)
        }
    }

    var actionsFor by remember { mutableStateOf<Scoop?>(null) }
    var confirmMarkAll by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val chrome = LocalChromeDirection.current

    fun refresh() {
        uiScope.launch {
            refreshing = true
            try {
                c.refresh()
            } finally {
                refreshing = false
            }
        }
    }

    ScrollHidingScaffold(
        atTop = { !c.listState.canScrollBackward },
        barHeight = HeaderHeight,
        bar = {
            Box(Modifier.fillMaxSize().background(p.bg)) {
                if (c.searching) {
                    Box(Modifier.align(Alignment.Center)) { SearchBar(c) }
                } else {
                    ScoopsHeader(c)
                }
                // Work still happening behind a list that already looks
                // finished (it paints from disk): the feed request, then the
                // card backfill.
                RefreshBar(meta.progress, c.feedLoading, Modifier.align(Alignment.BottomCenter))
            }
        },
    ) {
        // Content is always Hebrew → force RTL even if the UI language is English.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = ::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = HeaderHeight),
                    )
                },
            ) {
                ScaledText(SettingsController.listScale) {
                Content(
                    c,
                    bottomInset = bottomInset,
                    onRetry = ::refresh,
                    onMarkAll = { if (c.loaded.isNotEmpty()) confirmMarkAll = true },
                    onOpen = { s ->
                        view.selectionClick()
                        nav.push(ThreadRoute(s))
                    },
                    onToggleRead = { s ->
                        view.mediumImpact()
                        c.toggleRead(s)
                    },
                    onReply = { s ->
                        view.mediumImpact()
                        nav.home.scope.launch { openComposer(nav, ComposeRoute(threadId = s.id, parentNum = 0)) }
                    },
                    onLongPress = { s ->
                        view.mediumImpact()
                        actionsFor = s
                    },
                )
                }
            }
        }
        val pend = c.pending
        if (pend != null) {
            CompositionLocalProvider(LocalLayoutDirection provides chrome) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = HeaderHeight + 10.dp, start = 14.dp, end = 14.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    UpdatePill(c, pend) {
                        view.selectionClick()
                        meta.setOrderingPaused(false)
                        c.applyPending()
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = c.refreshFailed,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 76.dp),
        ) {
            Toast(l.refreshFailed)
        }
    }

    if (confirmMarkAll) {
        AlertDialog(
            onDismissRequest = { confirmMarkAll = false },
            text = { Text(l.markAllReadConfirm) },
            dismissButton = { TextButton(onClick = { confirmMarkAll = false }) { Text(l.cancel) } },
            confirmButton = {
                Button(onClick = {
                    confirmMarkAll = false
                    view.mediumImpact()
                    ReadStore.markAllRead(c.loaded.map { it.id })
                    Snacks.show(l.markedAllRead, 2000)
                }) { Text(l.markAllRead) }
            },
        )
    }

    actionsFor?.let { s ->
        val saved = SavedStore.saved.contains(s.id)
        ModalBottomSheet(onDismissRequest = { actionsFor = null }) {
            Column(Modifier.padding(bottom = 16.dp)) {
                SheetItem(if (saved) Icons.Rounded.BookmarkRemove else Icons.Outlined.BookmarkAdd, if (saved) l.unsaveScoop else l.saveScoop) {
                    actionsFor = null
                    SavedStore.saved.toggle(s)
                }
                SheetItem(Icons.Rounded.IosShare, l.share) {
                    actionsFor = null
                    shareText(context, s.url)
                }
                SheetItem(Icons.Rounded.OpenInBrowser, l.openOnRotter) {
                    actionsFor = null
                    openExternal(context, s.url)
                }
            }
        }
    }
}

@Composable
private fun SheetItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text, style = TextStyle(fontSize = 16.sp, color = palette.ink)) },
        leadingContent = { Icon(icon, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.combinedClickable(onClick = onClick),
    )
}

@Composable
private fun SearchBar(c: ScoopsController) {
    val l = strings
    val p = palette
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AppBar(
        titleSpacing = 12,
        title = {
            // An iOS-style search field: rounded, magnifier, clear button.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(p.field, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = p.muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (c.query.isEmpty()) {
                        Text(l.searchScoops, style = TextStyle(color = p.muted, fontSize = 17.sp), maxLines = 1)
                    }
                    BasicTextField(
                        value = c.query,
                        onValueChange = { c.query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = p.ink, fontSize = 17.sp, fontFamily = com.bennybarak.scoops.rotter_scoops.ui.NotoSansHebrew),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (c.query.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .combinedClickable(onClick = { c.query = "" })
                            .semantics { contentDescription = l.cancel },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Cancel, null, tint = p.muted, modifier = Modifier.size(20.dp)) }
                }
            }
        },
        actions = {
            IconButton(onClick = { c.stopSearch() }) { Icon(Icons.Rounded.Close, contentDescription = l.cancel) }
        },
    )
}

/** Height of the list's large header (title + search). */
private val HeaderHeight = 72.dp

/** A large "סקופים" title with the brand under-line, and a round search button. */
@Composable
private fun ScoopsHeader(c: ScoopsController) {
    val l = strings
    val p = palette
    Row(
        Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            l.tabScoops,
            style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.6).sp, color = p.ink),
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            l.brandRotter,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, color = p.muted),
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(p.field)
                .clickable { c.searching = true }
                .semantics { contentDescription = l.searchScoops },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Search, null, tint = p.ink) }
    }
}

/** The filter chips, all six, each with its count; scrolls sideways. */
@Composable
private fun FilterChips(c: ScoopsController) {
    val l = strings
    val p = palette
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
    ) {
        items(ScoopFilter.entries.size) { i ->
            val f = ScoopFilter.entries[i]
            val selected = c.filter == f
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) scheme.primary else p.surface)
                    .clickable {
                        if (!selected) {
                            view.selectionClick()
                            c.filter = f
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (selected) scheme.onPrimary else p.ink
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(filterLabel(f, l), style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W700, color = fg))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${c.count(f)}",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W600, color = fg.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

/** The sort control (with mark-all-read in its menu), shown beside the first day header. */
@Composable
private fun SortControl(c: ScoopsController, onMarkAll: () -> Unit) {
    val l = strings
    val p = palette
    val view = LocalView.current
    var menu by remember { mutableStateOf(false) }
    val sort = SettingsController.sortMode
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { menu = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (sort == SortMode.lastComment) l.sortLastComment else l.sortPostTime,
                style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = p.ink),
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.SwapVert, l.sortBy, tint = p.ink, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val pick: (() -> Unit) -> Unit = { action ->
                menu = false
                view.selectionClick()
                action()
            }
            Text(
                l.sortBy,
                style = TextStyle(fontSize = 12.5.sp, color = p.muted),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            CheckedItem(l.sortLastComment, sort == SortMode.lastComment) {
                pick { SettingsController.setSortMode(SortMode.lastComment) }
            }
            CheckedItem(l.sortPostTime, sort == SortMode.postTime) {
                pick { SettingsController.setSortMode(SortMode.postTime) }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(l.markAllRead, style = TextStyle(fontSize = 16.sp)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAddCheck, null, Modifier.size(20.dp)) },
                enabled = c.loaded.isNotEmpty(),
                onClick = { pick(onMarkAll) },
            )
        }
    }
}

@Composable
private fun CheckedItem(text: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = TextStyle(fontSize = 16.sp)) },
        leadingIcon = {
            if (checked) Icon(Icons.Rounded.Check, null) else Spacer(Modifier.size(24.dp))
        },
        onClick = onClick,
    )
}

@Composable
private fun UpdatePill(c: ScoopsController, pending: List<Scoop>, onTap: () -> Unit) {
    val l = strings
    val scheme = MaterialTheme.colorScheme
    val ids = remember(c.loaded) { c.loaded.map { it.id }.toSet() }
    val hasNew = pending.any { it.id !in ids }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(200)) }
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(999.dp),
        color = scheme.tertiaryContainer,
        contentColor = scheme.onTertiaryContainer,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = anim.value
                translationY = -(1 - anim.value) * 0.4f * size.height
            },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.ArrowUpward, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (hasNew) l.feedNewScoops else l.feedUpdated,
                style = TextStyle(fontWeight = FontWeight.W800, fontSize = 14.5.sp, color = scheme.onTertiaryContainer),
            )
        }
    }
}

@Composable
private fun Toast(text: String) {
    val p = palette
    Box(
        Modifier
            .dropShadowCompat(RoundedCornerShape(999.dp), Color.Black.copy(alpha = 0.12f), 14.dp, 0.dp)
            .background(p.surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = TextStyle(fontSize = 13.sp, color = p.ink))
    }
}

/**
 * A hairline under the app bar while the feed loads (indeterminate) and while
 * card metadata backfills (real progress). Holds its height when idle so the
 * list never nudges.
 */
@Composable
private fun RefreshBar(progress: Float?, feedLoading: Boolean, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val track = palette.field
    Box(modifier.fillMaxWidth().height(2.5.dp)) {
        if (progress == null && !feedLoading) return@Box
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                color = accent,
                trackColor = track,
                gapSize = 0.dp,
            )
        } else {
            // Eased, so an uneven jump doesn't read as a glitch.
            val v by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "progress")
            LinearProgressIndicator(
                progress = { v },
                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                color = accent,
                trackColor = track,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

private sealed interface Entry
private data class Header(val text: String, val first: Boolean) : Entry
/** A card and where it sits in its day's group (for the joined corners). */
private data class Item(val scoop: Scoop, val first: Boolean, val last: Boolean) : Entry

@Composable
private fun Content(
    c: ScoopsController,
    bottomInset: androidx.compose.ui.unit.Dp,
    onRetry: () -> Unit,
    onMarkAll: () -> Unit,
    onOpen: (Scoop) -> Unit,
    onToggleRead: (Scoop) -> Unit,
    onReply: (Scoop) -> Unit,
    onLongPress: (Scoop) -> Unit,
) {
    val l = strings
    if (c.loaded.isEmpty() && !c.didFetch && !c.failed) return SkeletonList()
    if (c.failed) {
        return MessageState(Icons.Rounded.CloudOff, l.loadingError) {
            FilledTonalButton(onClick = onRetry) { Text(l.retry) }
        }
    }
    val q = c.query.trim().lowercase()
    val filter = c.filter
    val sortMode = SettingsController.sortMode
    val times = ScoopMetaCache.instance.orderingTimes
    val scoops = c.source(filter).filter { (q.isEmpty() || it.title.lowercase().contains(q)) && c.matches(filter, it) }
    // Flat list: day headers interleaved with scoop cards, in sort order; each
    // day's cards form one joined group.
    val entries = remember(scoops, sortMode, times, l) {
        val sorted = scoops.sortedWith { a, b ->
            val x = c.effTime(b, sortMode, times).compareTo(c.effTime(a, sortMode, times))
            if (x != 0) x else b.id.compareTo(a.id)
        }
        val out = ArrayList<Entry>()
        var current: Bucket? = null
        sorted.forEachIndexed { i, s ->
            val b = bucketOf(c.effTime(s, sortMode, times))
            val startsGroup = b != current
            if (startsGroup) {
                current = b
                out.add(Header(bucketLabel(b, l), first = out.isEmpty()))
            }
            val next = sorted.getOrNull(i + 1)
            val endsGroup = next == null || bucketOf(c.effTime(next, sortMode, times)) != b
            out.add(Item(s, first = startsGroup, last = endsGroup))
        }
        out
    }
    val p = palette
    val h = LocalConfiguration.current.screenHeightDp
    LazyColumn(
        state = c.listState,
        modifier = Modifier.fillMaxSize(),
        // Top inset clears the overlaid (hideable) header; the bottom clears the
        // translucent tab bar the list scrolls behind.
        contentPadding = PaddingValues(top = HeaderHeight, bottom = bottomInset + 12.dp),
    ) {
        item(contentType = "chips") { FilterChips(c) }
        if (scoops.isEmpty()) {
            item(contentType = "empty") {
                // The chips stay above, so another filter is one tap away.
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height((h * 0.2f).dp))
                    val (icon, text) = when {
                        q.isNotEmpty() -> Icons.Rounded.SearchOff to l.noResults
                        filter != ScoopFilter.all -> filterIcon(filter) to emptyText(filter, l)
                        else -> Icons.Rounded.Inbox to l.emptyScoops
                    }
                    Icon(icon, null, tint = p.muted, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(text, style = TextStyle(color = p.muted))
                    if (q.isEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        if (filter != ScoopFilter.all) {
                            FilledTonalButton(onClick = { c.filter = ScoopFilter.all }) { Text(l.filterShowAll) }
                        } else {
                            FilledTonalButton(onClick = onRetry) { Text(l.retry) }
                        }
                    }
                }
            }
            return@LazyColumn
        }
        // No item keys: like the Flutter list, rows keep their pixel position
        // when the order changes instead of following a moved item.
        itemsIndexed(entries, contentType = { _, e -> if (e is Header) 0 else 1 }) { _, e ->
            when (e) {
                is Header -> Row(
                    Modifier.padding(start = 20.dp, end = 10.dp, top = if (e.first) 4.dp else 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        e.text,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800, letterSpacing = 0.4.sp, color = p.muted),
                        modifier = Modifier.weight(1f),
                    )
                    if (e.first) SortControl(c, onMarkAll)
                }
                is Item -> key(e.scoop.id) {
                    // Each day's group: fully rounded outer corners, softer inner ones.
                    val big = 28.dp
                    val small = 16.dp
                    val shape = RoundedCornerShape(
                        topStart = if (e.first) big else small,
                        topEnd = if (e.first) big else small,
                        bottomStart = if (e.last) big else small,
                        bottomEnd = if (e.last) big else small,
                    )
                    Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = if (e.last) 0.dp else 8.dp)) {
                        SwipeRow(
                            scoop = e.scoop,
                            shape = shape,
                            onToggleRead = { onToggleRead(e.scoop) },
                            onReply = { onReply(e.scoop) },
                            onOpen = { onOpen(e.scoop) },
                            onLongPress = { onLongPress(e.scoop) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageState(icon: ImageVector, text: String, action: (@Composable () -> Unit)? = null) {
    val p = palette
    val h = LocalConfiguration.current.screenHeightDp
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height((h * 0.3f).dp))
                Icon(icon, null, tint = p.muted, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text(text, style = TextStyle(color = p.muted))
                if (action != null) {
                    Spacer(Modifier.height(14.dp))
                    action()
                }
            }
        }
    }
}

/**
 * Swipe in the reading direction to reply, against it to toggle read. The card
 * slides out, the action runs, and the card slides back (it is never removed).
 */
@Composable
private fun SwipeRow(
    scoop: Scoop,
    shape: Shape,
    onToggleRead: () -> Unit,
    onReply: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    val l = strings
    val p = palette
    val accent = MaterialTheme.colorScheme.primary
    val read = ReadStore.isRead(scoop.id)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The drag position, updated in place on every move (not from coroutines,
    // which could land after the release and cancel the return animation).
    var x by remember { mutableFloatStateOf(0f) }
    var settle by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        // Positive = toward the reading direction's end (start→end = reply).
        val toEnd = if (rtl) -x else x
        if (x != 0f) {
            val replying = toEnd > 0
            SwipeBackground(
                color = if (replying) accent else if (read) p.muted else Mine,
                icon = if (replying) Icons.AutoMirrored.Rounded.Reply else if (read) Icons.Rounded.MarkEmailUnread else Icons.Rounded.CheckCircle,
                label = if (replying) l.reply else if (read) l.markUnread else l.markRead,
                alignStart = replying,
                shape = shape,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            Modifier
                // The swipe actions, for TalkBack and other services.
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(l.reply) { onReply(); true },
                        CustomAccessibilityAction(if (read) l.markUnread else l.markRead) { onToggleRead(); true },
                    )
                }
                // Absolute: `offset` mirrors x in RTL, which moved the card
                // against the finger (drag deltas are always screen-left/right).
                .absoluteOffset { IntOffset(x.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d -> x += d },
                    onDragStarted = { settle?.cancel() },
                    onDragStopped = { velocity ->
                        val dirToEnd = if (rtl) -x else x
                        val v = if (rtl) -velocity else velocity
                        val flungToEnd = v > 700 * density.density && dirToEnd > 0
                        val flungToStart = v < -700 * density.density && dirToEnd < 0
                        val past = abs(x) > width * 0.4f
                        settle = scope.launch {
                            if (past || flungToEnd || flungToStart) {
                                // Slide out, act, then come back: the card is never removed.
                                animate(x, if (x > 0) width else -width, animationSpec = tween(200)) { value, _ -> x = value }
                                if (dirToEnd > 0) onReply() else onToggleRead()
                            }
                            animate(x, 0f, animationSpec = tween(200)) { value, _ -> x = value }
                        }
                    },
                ),
        ) {
            ScoopCard(scoop, read, shape, onOpen, onLongPress)
        }
    }
}

@Composable
private fun SwipeBackground(color: Color, icon: ImageVector, label: String, alignStart: Boolean, shape: Shape, modifier: Modifier) {
    Box(
        modifier
            .background(color, shape)
            .padding(horizontal = 22.dp),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(label, style = TextStyle(color = Color.White, fontWeight = FontWeight.W800))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoopCard(scoop: Scoop, read: Boolean, shape: Shape, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val p = palette
    val mine = MyRepliesStore.replied(scoop.id)
    val cache = ScoopMetaCache.instance
    cache.observe(scoop.id)
    SideEffect { cache.ensure(scoop.id) }
    val alpha by animateFloatAsState(if (read) 0.6f else 1f, tween(220), label = "read")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        Modifier
            .clip(shape)
            // Read cards sit a tone down from unread ones, as well as fading.
            .background(if (read) p.surface.copy(alpha = 0.55f).compositeOver(p.bg) else p.surface)
            .drawWithContent {
                drawContent()
                // Green side when the user has replied in this thread.
                if (mine) {
                    val w = 4.dp.toPx()
                    drawRect(Mine, topLeft = Offset(if (rtl) size.width - w else 0f, 0f), size = Size(w, size.height))
                }
            }
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
    ) {
        Column(Modifier.alpha(alpha)) {
            PosterLine(scoop)
            Spacer(Modifier.height(8.dp))
            Text(
                scoop.title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 1.3.em,
                    fontSize = 17.5.sp,
                    fontWeight = if (read) FontWeight.W600 else FontWeight.W800,
                    letterSpacing = (-0.2).sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            CardFooter(scoop, read)
        }
    }
}

/** A grey stand-in the size of the value it's waiting for, so the card holds its height. */
@Composable
private fun Placeholder(w: Int, h: Int) {
    Box(Modifier.size(w.dp, h.dp).background(palette.field, RoundedCornerShape(5.dp)))
}

/**
 * Poster (avatar + name + points) above the headline, posting time opposite.
 * Deliberately not tappable — the card has one action, opening the thread.
 */
@Composable
private fun PosterLine(scoop: Scoop) {
    val p = palette
    val l = strings
    val lang = LocalLanguage.current
    val cache = ScoopMetaCache.instance
    val meta = cache.of(scoop.id)
    val pending = cache.isPending(scoop.id)
    val author = meta?.author
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (author != null) {
            AvatarBubble(author, 21.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                author,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.W700, color = p.ink),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (meta.authorPoints != null) {
                Spacer(Modifier.width(6.dp))
                PointsChip(meta.authorPoints, p.muted, l.memberPoints, null)
            }
        } else if (pending) {
            Box(Modifier.size(21.dp).background(p.field, CircleShape))
            Spacer(Modifier.width(7.dp))
            Placeholder(84, 12)
        }
        Spacer(Modifier.weight(1f))
        if (scoop.published != null) {
            Text(
                relTime(scoop.published, l, lang),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W600, color = p.muted),
            )
        }
    }
}

/**
 * Status (new / new comments / read / removed) · replies · last activity, and
 * the one-tap bookmark. Author and replies arrive lazily.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(scoop: Scoop, read: Boolean) {
    val l = strings
    val p = palette
    val lang = LocalLanguage.current
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme
    val style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W600, color = p.muted)
    val cache = ScoopMetaCache.instance
    val meta = cache.of(scoop.id)
    val pending = cache.isPending(scoop.id)
    val isNew = ReadStore.isNew(scoop.id)
    // A 404 on an older scoop = removed (deleted by a moderator); it lingers in
    // the RSS feed until that regenerates. A FRESH 404 is just a page rotter
    // hasn't generated yet.
    val removed = cache.isUnavailable(scoop.id) && scoop.published != null &&
        System.currentTimeMillis() - scoop.published > 15 * 60_000
    val saved = SavedStore.saved.contains(scoop.id)
    Row(verticalAlignment = Alignment.CenterVertically) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                removed -> Pill(l.removedBadge, Danger, Icons.Rounded.DeleteOutline)
                isNew -> Pill(l.newCommentsBadge, scheme.primary, null)
                !read -> Pill(l.newBadge, scheme.primary, null)
                else -> IconText(Icons.Rounded.DoneAll, l.markRead, style, p.muted)
            }
            if (meta?.replies != null) {
                IconText(Icons.Outlined.ModeComment, "${meta.replies}", style, p.muted)
            } else if (pending) {
                Placeholder(30, 11)
            }
            // When the thread was last active — what "sort by last comment"
            // orders on; a clock, so it doesn't read as another count.
            if (meta?.lastComment != null) {
                IconText(Icons.Rounded.Schedule, relTime(meta.lastComment, l, lang), style, p.muted)
            } else if (pending) {
                Placeholder(56, 11)
            }
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable {
                    view.selectionClick()
                    SavedStore.saved.toggle(scoop)
                }
                .semantics { contentDescription = if (saved) l.unsaveScoop else l.saveScoop },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                null,
                tint = if (saved) scheme.primary else p.muted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Pill(text: String, c: Color, icon: ImageVector?) {
    Row(
        Modifier
            .background(c.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800, color = c))
    }
}

@Composable
private fun IconText(icon: ImageVector, text: String, style: TextStyle, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = style)
    }
}

@Composable
private fun SkeletonList() {
    val p = palette
    val t = rememberInfiniteTransition(label = "shimmer")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "phase")
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp + BarHeight, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = true,
    ) {
        items(7) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(p.surface, RoundedCornerShape(20.dp))
                    .drawWithContent {
                        drawContent()
                        // A soft band sweeping across, like the Flutter shimmer.
                        val x = size.width * (phase * 1.6f - 0.3f)
                        drawRect(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color.Transparent, p.muted.copy(alpha = 0.12f), Color.Transparent),
                                startX = x - size.width * 0.3f,
                                endX = x + size.width * 0.3f,
                            ),
                        )
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(14.dp).background(p.field, RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(9.dp))
                Box(Modifier.size(220.dp, 14.dp).background(p.field, RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(14.dp))
                Box(Modifier.size(140.dp, 11.dp).background(p.field, RoundedCornerShape(6.dp)))
            }
        }
    }
}
