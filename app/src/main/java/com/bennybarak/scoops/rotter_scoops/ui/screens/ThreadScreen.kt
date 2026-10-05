package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.SubdirectoryArrowLeft
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.data.Message
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.ReadStore
import com.bennybarak.scoops.rotter_scoops.data.ReadingStore
import com.bennybarak.scoops.rotter_scoops.data.SavedStore
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.data.Thread
import com.bennybarak.scoops.rotter_scoops.net.RotterHttpException
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.ui.ComposeRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalChromeDirection
import com.bennybarak.scoops.rotter_scoops.ui.LocalLanguage
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.Mine
import com.bennybarak.scoops.rotter_scoops.ui.Navigator
import com.bennybarak.scoops.rotter_scoops.ui.NotoSansHebrew
import com.bennybarak.scoops.rotter_scoops.ui.ProfileRoute
import com.bennybarak.scoops.rotter_scoops.ui.SummaryRoute
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AvatarBubble
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BackButton
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarHeight
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.HtmlBlocks
import com.bennybarak.scoops.rotter_scoops.ui.widgets.PointsChip
import com.bennybarak.scoops.rotter_scoops.ui.widgets.PostBody
import com.bennybarak.scoops.rotter_scoops.ui.widgets.ScrollHidingScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.dropShadowCompat
import com.bennybarak.scoops.rotter_scoops.ui.widgets.relTime
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/** One visible row of the flattened comment tree. */
data class FlatRow(
    val msg: Message,
    val depth: Int,
    val childCount: Int,
    val collapsed: Boolean,
    // Who this answers, when that's another commenter (null for replies to the
    // root). Past the indent cap it's the only cue tying a reply to its parent.
    val parentAuthor: String?,
)

/** The thread screen's state and logic; owned by its route. */
class ThreadState(val scoop: Scoop, private val scope: CoroutineScope) {
    companion object {
        private const val HINT_KEY = "threadNavigationExplained"

        /** Bodies parsed before the thread is shown (a few screens' worth). */
        private const val WARM_FIRST = 30

        /** Background warming stops here, well inside the 600-entry layout cache. */
        private const val WARM_MAX = 400
    }

    var thread by mutableStateOf<Thread?>(null); private set
    var error by mutableStateOf<Throwable?>(null); private set
    var loading by mutableStateOf(true); private set

    // Index-addressable scrolling so we can jump to a specific comment row.
    // List index 0 is the root card; row i is at list index i + 1.
    val listState = LazyListState()
    private val collapsed = mutableStateMapOf<Int, Unit>()
    val flat: List<FlatRow> by derivedStateOf { thread?.let { flatten(it) } ?: emptyList() }

    /**
     * Highest message number seen on the previous visit, fixed for this
     * screen's life so a refresh after posting doesn't wipe the "new" markers.
     */
    private var visitBaseline: Int? = null
    var newNums by mutableStateOf<Set<Int>>(emptySet()); private set
    var lastNew by mutableStateOf<Int?>(null); private set // the new comment last jumped to

    /**
     * Where the reader is (message number, 0 = the root): updated when a scroll
     * settles and on every jump, and remembered across visits.
     */
    var position by mutableStateOf<Int?>(null); private set
    var resumeTarget by mutableStateOf<Int?>(null) // offered as "Resume reading" on open
    private var restored = false

    /** Comments jumped away from via "replying to", for "Back to reply". */
    val returnStack = mutableStateListOf<Int>()
    var highlighted by mutableStateOf<Int?>(null); private set
    private var highlightJob: Job? = null

    var searching by mutableStateOf(false); private set
    var searchText by mutableStateOf("")
    private var searchJob: Job? = null
    private var searchIndex: List<Pair<Int, String>>? = null // (num, lowercase text) in display order
    var matches by mutableStateOf<List<Int>>(emptyList()); private set
    var matchIndex by mutableIntStateOf(0); private set
    var searchPending by mutableStateOf(false); private set

    var hintDismissed by mutableStateOf(Prefs.getBool(HINT_KEY) ?: false); private set
    private var userScrolling = false

    /** Set by the screen: the haptic tick a jump plays. */
    var tick: () -> Unit = {}

    init {
        scope.launch { load() }
    }

    fun toggleCollapse(num: Int) {
        tick()
        if (collapsed.remove(num) == null) collapsed[num] = Unit
    }

    val allCollapsed: Boolean
        get() = flat.isNotEmpty() && flat.all { it.depth == 0 } && flat.any { it.collapsed }

    fun toggleCollapseAll() {
        val t = thread ?: return
        tick()
        if (allCollapsed) {
            collapsed.clear()
        } else {
            collapsed.clear()
            for (m in t.childrenOf(0)) collapsed[m.num] = Unit
        }
    }

    private fun indexOf(num: Int): Int {
        if (num == 0) return 0
        val i = flat.indexOfFirst { it.msg.num == num }
        return if (i < 0) -1 else i + 1
    }

    /** Nested replies can be inside collapsed branches: open their ancestors. */
    private fun reveal(num: Int) {
        val byNum = thread!!.messages.associateBy { it.num }
        var cur = num
        val seen = HashSet<Int>()
        while (seen.add(cur)) {
            collapsed.remove(cur)
            val parent = byNum[cur]?.parent
            if (parent == null || parent == 0) break
            cur = parent
        }
    }

    /**
     * Scroll to message [num] and tint it briefly, so the reader can see where
     * they landed instead of working it out from the surrounding text.
     */
    fun jump(num: Int) {
        val t = thread ?: return
        if (t.messages.none { it.num == num }) return
        tick()
        reveal(num)
        resumeTarget = null
        if (newNums.contains(num)) lastNew = num
        position = num
        highlighted = num
        ReadingStore.rememberPosition(scoop.id, num)
        highlightJob?.cancel()
        highlightJob = scope.launch {
            delay(1400)
            highlighted = null
        }
        scope.launch {
            // After the revealed rows are laid out.
            withFrameNanos { }
            withFrameNanos { }
            val index = indexOf(num)
            if (index < 0) return@launch
            // The list's top padding lands the row just below the overlaid bar.
            listState.animateScrollToItem(index)
        }
    }

    /** The message at the top of the viewport (0 = the root card). */
    fun topVisibleNum(): Int {
        val onScreen = listState.layoutInfo.visibleItemsInfo.filter { it.offset + it.size > 0 }
        if (onScreen.isEmpty()) return 0
        val index = onScreen.minOf { it.index }
        return if (index == 0 || index > flat.size) 0 else flat[index - 1].msg.num
    }

    /** The reader took over: drop the offers and the landing tint. */
    fun onDragStart() {
        resumeTarget = null
        highlighted = null
        userScrolling = true
    }

    fun onScrollSettled() {
        if (!userScrolling) return
        userScrolling = false
        val pos = topVisibleNum()
        position = pos
        ReadingStore.rememberPosition(scoop.id, pos)
    }

    /**
     * The next top-level comment below the current position — stopping at the
     * end rather than unexpectedly wrapping back to the top.
     */
    fun nextDiscussion(): Int? {
        val pos = position ?: topVisibleNum()
        val start = if (pos == 0) 0 else flat.indexOfFirst { it.msg.num == pos } + 1
        for (i in start until flat.size) if (flat[i].depth == 0) return flat[i].msg.num
        return null
    }

    /**
     * Loads the thread, catching errors into state so they're shown in-UI (a
     * fresh thread's `.shtml` snapshot can 404 until rotter generates it).
     */
    suspend fun load() {
        loading = true
        error = null
        try {
            val id = scoop.id
            val t = RotterService.fetchThread(id)
            // Parse the first screenfuls' bodies off the main thread before
            // showing the thread (so its first frames don't), and the rest
            // after it is on screen — never the whole thread up front, which
            // delayed first content and could overflow the layout cache.
            val reading = listOfNotNull(t.root) + flatten(t).map { it.msg }
            val bodies = reading.mapNotNull { it.bodyHtml }
            withContext(Dispatchers.Default) {
                for (b in bodies.take(WARM_FIRST)) HtmlBlocks.parse(b, scoop.url, true)
            }
            if (bodies.size > WARM_FIRST) {
                scope.launch(Dispatchers.Default) {
                    for (b in bodies.subList(WARM_FIRST, minOf(bodies.size, WARM_MAX))) {
                        HtmlBlocks.parse(b, scoop.url, true)
                    }
                }
            }
            val latest = t.comments.maxOfOrNull { it.num }?.coerceAtLeast(0) ?: 0
            // A first-ever visit sets a baseline; it doesn't label everything new.
            if (visitBaseline == null) visitBaseline = ReadingStore.latestMessage(id) ?: latest
            val baseline = visitBaseline!!
            ReadingStore.recordVisit(id, latest)
            // Opening a thread marks it read at its current reply count, so the
            // list dulls it and only re-highlights once newer comments arrive.
            ReadStore.markRead(id, t.comments.size)
            thread = t
            loading = false
            newNums = t.comments.filter { it.num > baseline }.map { it.num }.toSet()
            searchIndex = null
            if (!restored) {
                restored = true
                val saved = ReadingStore.position(id)
                if (saved != null && saved != 0) {
                    val nums = t.comments.map { it.num }.sorted()
                    resumeTarget = if (nums.contains(saved)) saved else nums.firstOrNull { it > saved }
                }
            }
            if (searching) runSearch(searchText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e
            loading = false
        }
    }

    /**
     * Depth-first flatten of the comment tree, skipping descendants of
     * collapsed nodes (unless [all]) and carrying what each row needs.
     */
    fun flatten(t: Thread, all: Boolean = false): List<FlatRow> {
        val authors = t.messages.associate { it.num to it.author }
        val out = ArrayList<FlatRow>()
        fun walk(parentNum: Int, depth: Int) {
            for (child in t.childrenOf(parentNum)) {
                val isCollapsed = collapsed.containsKey(child.num)
                val p = child.parent ?: 0
                out.add(
                    FlatRow(
                        msg = child,
                        depth = depth,
                        childCount = t.childrenOf(child.num).size,
                        collapsed = isCollapsed,
                        parentAuthor = if (p == 0) null else authors[p],
                    ),
                )
                if (all || !isCollapsed) walk(child.num, depth + 1)
            }
        }
        walk(0, 0)
        return out
    }

    // ---- Search ---------------------------------------------------------------

    fun openSearch() {
        searching = true
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchText = ""
        searching = false
        matches = emptyList()
        searchPending = false
    }

    fun onSearchChanged(q: String) {
        searchText = q
        searchJob?.cancel()
        searchPending = q.isNotBlank()
        matches = emptyList()
        searchJob = scope.launch {
            delay(250)
            runSearch(q)
        }
    }

    /**
     * Author, title and body text, in reading order. The plain-text index is
     * built once per load off the main thread — bodies are HTML.
     */
    private suspend fun runSearch(raw: String) {
        val q = raw.trim().lowercase()
        val t = thread
        if (q.isEmpty() || t == null) {
            matches = emptyList()
            searchPending = false
            return
        }
        if (searchIndex == null) {
            val ordered = listOfNotNull(t.root) + flatten(t, all = true).map { it.msg }
            searchIndex = withContext(Dispatchers.Default) {
                ordered.map { m ->
                    val body = Jsoup.parseBodyFragment(m.bodyHtml ?: "").body().wholeText()
                    m.num to "${m.author}\n${m.title ?: ""}\n$body".lowercase()
                }
            }
        }
        if (searchText.trim().lowercase() != q) return
        val found = searchIndex!!.filter { it.second.contains(q) }.map { it.first }
        matches = found
        matchIndex = 0
        searchPending = false
        if (found.isNotEmpty()) jump(found.first())
    }

    fun stepMatch(delta: Int) {
        val i = matchIndex + delta
        if (i < 0 || i >= matches.size) return
        matchIndex = i
        jump(matches[i])
    }

    // ---- Actions ----------------------------------------------------------------

    fun openUserProfile(nav: Navigator, m: Message) {
        tick()
        val posts = thread?.messages?.filter { it.author == m.author } ?: listOf(m)
        // The member stats repeat on every post by that author; use whichever carries them.
        val stats = posts.firstOrNull { it.points != null } ?: m
        nav.push(
            ProfileRoute(
                name = m.author,
                joinDate = stats.joinDate,
                messages = stats.messages,
                raters = stats.raters,
                points = stats.points,
                posts = posts,
                baseUrl = scoop.url,
            ),
        )
    }

    fun reply(nav: Navigator, parentNum: Int) {
        scope.launch {
            val posted = openComposer(nav, ComposeRoute(threadId = scoop.id, parentNum = parentNum))
            // Re-fetch so a just-posted reply appears and the read baseline advances.
            if (posted == true) load()
        }
    }

    /** Edit one of the user's own messages ([num] 0 = the root post). */
    fun edit(nav: Navigator, num: Int) {
        scope.launch {
            val saved = openComposer(nav, ComposeRoute(threadId = scoop.id, editNum = num))
            if (saved == true) load()
        }
    }

    fun summarize(nav: Navigator) {
        val t = thread ?: return
        nav.push(SummaryRoute(t, t.root?.title ?: scoop.title))
    }

    fun dismissHint() {
        hintDismissed = true
        Prefs.setBool(HINT_KEY, true)
    }

    fun refresh() = scope.launch { load() }

    fun jumpClearingReturn(num: Int) {
        returnStack.clear()
        jump(num)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(s: ThreadState) {
    val l = strings
    val view = LocalView.current
    s.tick = { view.selectionClick() }
    val chrome = LocalChromeDirection.current

    LaunchedEffect(s.listState) {
        s.listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) s.onDragStart()
        }
    }
    LaunchedEffect(s.listState) {
        snapshotFlow { s.listState.isScrollInProgress }
            .filter { !it }
            .collect { s.onScrollSettled() }
    }

    ScrollHidingScaffold(
        pinned = s.searching,
        atTop = { !s.listState.canScrollBackward },
        bar = { if (s.searching) ThreadSearchBar(s) else ThreadTitleBar(s) },
        floatingActions = { Fabs(s) },
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val pull = rememberPullToRefreshState()
            var refreshing by remember { mutableStateOf(false) }
            val ui = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    ui.launch {
                        refreshing = true
                        try {
                            s.load()
                        } finally {
                            refreshing = false
                        }
                    }
                },
                state = pull,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pull,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = BarHeight),
                    )
                },
            ) {
                ThreadBody(s)
            }
        }
        val resume = s.resumeTarget
        if (resume != null) {
            CompositionLocalProvider(LocalLayoutDirection provides chrome) {
                ResumeBanner(
                    onResume = { s.jump(resume) },
                    onTop = { s.resumeTarget = null },
                    modifier = Modifier.padding(top = BarHeight + 6.dp, start = 12.dp, end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ThreadTitleBar(s: ThreadState) {
    val l = strings
    val nav = LocalNav.current
    val view = LocalView.current
    val t = s.thread
    var menu by remember { mutableStateOf(false) }
    AppBar(
        leading = { BackButton() },
        title = { BarTitle(l.tabScoops) },
        actions = {
            if (t != null) {
                IconButton(onClick = { s.openSearch() }) { Icon(Icons.Rounded.Search, l.searchThread) }
            }
            // Only when summaries are on and a key is stored — otherwise the
            // button would promise something the app can't do.
            if (t != null && AIStore.isReady) {
                IconButton(onClick = { s.summarize(nav) }) { Icon(Icons.Rounded.AutoAwesome, l.aiSummarize) }
            }
            val saved = SavedStore.saved.contains(s.scoop.id)
            IconButton(onClick = {
                view.selectionClick()
                SavedStore.saved.toggle(s.scoop)
            }) {
                Icon(
                    if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    if (saved) l.unsaveScoop else l.saveScoop,
                )
            }
            if (t != null) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        val following = SavedStore.followed.contains(s.scoop.id)
                        DropdownMenuItem(
                            text = { Text(if (following) l.unfollow else l.follow, style = TextStyle(fontSize = 16.sp)) },
                            leadingIcon = {
                                Icon(if (following) Icons.Outlined.NotificationsOff else Icons.Rounded.NotificationsNone, null)
                            },
                            onClick = {
                                menu = false
                                SavedStore.followed.toggle(s.scoop)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (s.allCollapsed) l.expandAll else l.collapseAll, style = TextStyle(fontSize = 16.sp)) },
                            leadingIcon = {
                                Icon(if (s.allCollapsed) Icons.Rounded.UnfoldMore else Icons.Rounded.UnfoldLess, null)
                            },
                            enabled = t.comments.isNotEmpty(),
                            onClick = {
                                menu = false
                                s.toggleCollapseAll()
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ThreadSearchBar(s: ThreadState) {
    val l = strings
    val p = palette
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hasQuery = s.searchText.isNotBlank()
    AppBar(
        titleSpacing = 12,
        title = {
            TextField(
                value = s.searchText,
                onValueChange = s::onSearchChanged,
                singleLine = true,
                placeholder = { Text(l.searchThreadHint, style = TextStyle(fontSize = 16.sp)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                textStyle = TextStyle(fontSize = 16.sp, color = p.ink, fontFamily = NotoSansHebrew),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.field,
                    unfocusedContainerColor = p.field,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focus),
            )
        },
        actions = {
            if (s.searchPending) {
                Box(Modifier.padding(horizontal = 8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            } else if (hasQuery) {
                Text(
                    if (s.matches.isEmpty()) l.matchesCount(0) else "${s.matchIndex + 1} / ${s.matches.size}",
                    style = TextStyle(fontSize = 12.5.sp, color = p.muted, textDirection = TextDirection.Ltr),
                )
            }
            IconButton(
                onClick = { s.stepMatch(-1) },
                enabled = !s.searchPending && s.matchIndex != 0,
            ) { Icon(Icons.Rounded.KeyboardArrowUp, l.previousMatch) }
            IconButton(
                onClick = { s.stepMatch(1) },
                enabled = !s.searchPending && s.matchIndex + 1 < s.matches.size,
            ) { Icon(Icons.Rounded.KeyboardArrowDown, l.nextMatch) }
            IconButton(onClick = { s.closeSearch() }) { Icon(Icons.Rounded.Close, l.close) }
        },
    )
}

@Composable
private fun ResumeBanner(onResume: () -> Unit, onTop: () -> Unit, modifier: Modifier) {
    val l = strings
    val p = palette
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = p.surface,
        shadowElevation = 3.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onResume) {
                Icon(Icons.Rounded.Bookmark, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(l.resumeReading)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onTop) { Text(l.startTop, color = p.muted) }
        }
    }
}

@Composable
private fun Fabs(s: ThreadState) {
    val t = s.thread ?: return
    if (t.comments.isEmpty()) return
    val l = strings
    val p = palette
    val accent = MaterialTheme.colorScheme.primary
    val me = AuthService.username
    val myNum = if (me.isNullOrEmpty()) null else t.comments.firstOrNull { it.author == me }?.num
    // Message numbers grow as replies are posted, whatever their tree position.
    val newest = t.comments.maxOf { it.num }.coerceAtLeast(0)
    val next = s.nextDiscussion()
    val news = s.newNums.sorted()
    val lastNew = s.lastNew
    val newIndex = if (lastNew == null) -1 else news.indexOf(lastNew)
    val prevNew = if (lastNew == null) null else news.lastOrNull { it < lastNew }
    val nextNew = if (lastNew == null) news.firstOrNull() else news.firstOrNull { it > lastNew }

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!s.hintDismissed) {
            // Not a Surface: that swallows every touch, so the comments under the
            // card couldn't be scrolled. Only the close button takes input.
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .widthIn(max = 250.dp)
                    .dropShadowCompat(shape, Color.Black.copy(alpha = 0.18f), 6.dp, 2.dp)
                    .background(p.surface, shape),
            ) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        l.navigationHint,
                        style = TextStyle(fontSize = 12.5.sp, color = p.ink, lineHeight = 1.4.em),
                        modifier = Modifier.weight(1f).padding(top = 4.dp),
                    )
                    IconButton(onClick = { s.dismissHint() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, l.close, Modifier.size(18.dp))
                    }
                }
            }
        }
        if (s.returnStack.isNotEmpty()) {
            Surface(
                onClick = { s.jump(s.returnStack.removeAt(s.returnStack.lastIndex)) },
                color = p.surface,
                shadowElevation = 3.dp,
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.UTurnLeft, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(l.backToReply, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink))
                }
            }
        }
        // Everything that moves through the thread, in ONE compact floating
        // toolbar in the corner, tinted with the theme's accent container.
        val scheme = MaterialTheme.colorScheme
        val onBar = scheme.onPrimaryContainer
        Surface(color = scheme.primaryContainer, contentColor = onBar, shadowElevation = 3.dp, shape = CircleShape) {
            Row(
                Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (news.isNotEmpty()) {
                    // The new-comments stepper, on its own tonal segment.
                    Row(
                        Modifier
                            .height(40.dp)
                            .background(onBar.copy(alpha = 0.08f), CircleShape),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolbarIcon(Icons.Rounded.KeyboardArrowUp, l.previousNew, prevNew != null) {
                            prevNew?.let(s::jumpClearingReturn)
                        }
                        Text(
                            if (newIndex >= 0) l.newOfCount(newIndex + 1, news.size) else l.newRepliesCount(news.size),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800, color = onBar),
                            maxLines = 1,
                        )
                        ToolbarIcon(Icons.Rounded.KeyboardArrowDown, l.nextNew, nextNew != null) {
                            nextNew?.let(s::jumpClearingReturn)
                        }
                    }
                }
                if (myNum != null) {
                    ToolbarCircle(Mine, Color.White, Icons.AutoMirrored.Rounded.Reply, l.myReply, true) {
                        s.jumpClearingReturn(myNum)
                    }
                }
                ToolbarIcon(Icons.Rounded.South, l.jumpToNewest, true) { s.jumpClearingReturn(newest) }
                // Next discussion, the main action: the filled accent circle.
                // Dimmed at the last discussion rather than wrapping to the top.
                ToolbarCircle(scheme.primary, scheme.onPrimary, Icons.Rounded.KeyboardArrowDown, l.nextComment, next != null) {
                    next?.let(s::jumpClearingReturn)
                }
            }
        }
    }
}

/** A filled 40dp circle button in the thread toolbar. */
@Composable
private fun ToolbarCircle(
    fill: Color,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fill.copy(alpha = if (enabled) 1f else 0.38f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
}

/** A 40dp round icon button in the thread toolbar. */
@Composable
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ThreadBody(s: ThreadState) {
    val l = strings
    val p = palette
    val t = s.thread
    if (s.loading && t == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val err = s.error
    if (err != null && t == null) {
        // A 404 is either a brand-new post whose static .shtml isn't generated
        // yet, or a thread that was removed. Tell them apart by age.
        val is404 = err is RotterHttpException && err.isNotFound
        val published = s.scoop.published
        val fresh = published != null && System.currentTimeMillis() - published < 15 * 60_000
        val removed = is404 && !fresh
        val notReady = is404 && fresh
        val h = LocalConfiguration.current.screenHeightDp
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height((h * 0.28f).dp))
                    Icon(
                        when {
                            removed -> Icons.Rounded.DeleteOutline
                            notReady -> Icons.Rounded.HourglassEmpty
                            else -> Icons.Rounded.CloudOff
                        },
                        null,
                        tint = p.muted,
                        modifier = Modifier.size(46.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            removed -> l.scoopRemoved
                            notReady -> l.threadNotReady
                            else -> l.loadingError
                        },
                        style = TextStyle(color = p.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    // A removed thread won't come back, so only offer retry when it might.
                    if (!removed) FilledTonalButton(onClick = { s.refresh() }) { Text(l.retry) }
                }
            }
        }
        return
    }
    if (t == null) return
    val nav = LocalNav.current
    val root = t.root
    val me = AuthService.username
    val flat = s.flat
    val density = SettingsController.threadDensity.toFloat()
    LazyColumn(
        state = s.listState,
        modifier = Modifier.fillMaxSize(),
        // Top inset clears the overlaid (hideable) app bar; extra bottom
        // clearance so the FAB stack doesn't cover the last post.
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp + BarHeight, bottom = 160.dp),
    ) {
        item(key = "root", contentType = "root") {
            RootCard(
                scoop = s.scoop,
                root = root,
                replyCount = t.comments.size,
                isMine = !me.isNullOrEmpty() && root?.author == me,
                onReply = { s.reply(nav, 0) },
                onEdit = { s.edit(nav, 0) },
                onOpenUser = if (root == null) null else ({ s.openUserProfile(nav, root) }),
            )
        }
        itemsIndexed(flat, key = { _, r -> r.msg.num }, contentType = { _, _ -> "comment" }) { _, row ->
            CommentTile(
                row = row,
                isOp = root != null && row.msg.author == root.author,
                isMine = !me.isNullOrEmpty() && row.msg.author == me,
                isNew = s.newNums.contains(row.msg.num),
                highlighted = s.highlighted == row.msg.num,
                density = density,
                baseUrl = s.scoop.url,
                onReply = { s.reply(nav, row.msg.num) },
                onEdit = { s.edit(nav, row.msg.num) },
                onToggleCollapse = { s.toggleCollapse(row.msg.num) },
                onOpenUser = { s.openUserProfile(nav, row.msg) },
                onJumpToParent = {
                    val parent = row.msg.parent
                    if (parent != null) {
                        s.returnStack.add(row.msg.num)
                        s.jump(parent)
                    }
                },
            )
        }
    }
}

@Composable
private fun RootCard(
    scoop: Scoop,
    root: Message?,
    replyCount: Int,
    isMine: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onOpenUser: (() -> Unit)?,
) {
    val l = strings
    val p = palette
    val lang = LocalLanguage.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .padding(bottom = 14.dp)
            .shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .background(p.surface, shape)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Text(
            root?.title ?: scoop.title,
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 1.3.em),
        )
        Spacer(Modifier.height(14.dp))
        val time = if (scoop.published != null) relTime(scoop.published, l, lang) else (root?.time ?: "")
        val timeStyle = TextStyle(color = p.muted, fontSize = 12.5.sp)
        if (root == null) {
            Text(time, style = timeStyle)
        } else {
            // Tap avatar/name → native profile.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBubble(
                    root.author,
                    34.dp,
                    Modifier.clip(CircleShape).clickable(enabled = onOpenUser != null) { onOpenUser?.invoke() },
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            root.author,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontWeight = FontWeight.W800, color = p.ink, fontSize = 15.sp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = onOpenUser != null) { onOpenUser?.invoke() },
                        )
                        if (root.points != null) {
                            Spacer(Modifier.width(8.dp))
                            PointsChip(root.points, p.muted, l.memberPoints, onOpenUser)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(time, style = timeStyle)
                }
            }
        }
        if (root?.bodyHtml != null) {
            Spacer(Modifier.height(14.dp))
            BodyParagraphs(root.bodyHtml, scoop.url)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ModeComment, null, tint = p.muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(l.replies(replyCount), style = TextStyle(fontWeight = FontWeight.W700, color = p.muted, fontSize = 13.sp))
            Spacer(Modifier.weight(1f))
            // Only your own post can be edited.
            if (isMine) {
                CompactTextButton(onEdit, p.muted, Icons.Rounded.Edit, l.edit, 17, 14f)
            }
            Spacer(Modifier.width(4.dp))
            // Enabled when signed out too — it signs in first.
            FilledTonalButton(onClick = onReply, contentPadding = PaddingValues(start = 16.dp, end = 24.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Reply, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(l.reply)
            }
        }
    }
}

private val brSplit = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

/**
 * Render the OP body as paragraphs (split on the single `<br>` rotter leaves
 * between blocks) separated by a blank line of breathing room.
 */
@Composable
private fun BodyParagraphs(html: String, baseUrl: String) {
    val parts = remember(html) {
        html.split(brSplit).map { it.trim() }.filter { it.isNotEmpty() && it != "&nbsp;" }
    }
    if (parts.size < 2) {
        PostBody(html, baseUrl)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (part in parts) PostBody(part, baseUrl)
    }
}

@Composable
private fun CompactTextButton(
    onClick: () -> Unit,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String,
    iconSize: Int = 16,
    fontSize: Float = 12.5f,
    bold: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(iconSize.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = TextStyle(fontSize = fontSize.sp, fontWeight = if (bold) FontWeight.W700 else FontWeight.W500))
    }
}

/** The indent stops growing here — past it a Hebrew line would be squeezed too narrow. */
private const val MAX_INDENT = 6

@Composable
private fun CommentTile(
    row: FlatRow,
    isOp: Boolean,
    isMine: Boolean,
    isNew: Boolean, // posted since the previous visit
    highlighted: Boolean, // just jumped to
    density: Float,
    baseUrl: String,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onToggleCollapse: () -> Unit,
    onOpenUser: () -> Unit,
    onJumpToParent: () -> Unit,
) {
    val l = strings
    val p = palette
    val lang = LocalLanguage.current
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary
    val message = row.msg
    val depth = row.depth
    val collapsed = row.collapsed
    val clamped = depth.coerceIn(0, MAX_INDENT)
    val hasKids = row.childCount > 0
    val railColor = if (isMine) Mine else if (depth == 0) accent else accent.copy(alpha = 0.45f)
    val author = message.author.trim()
    val muted = p.muted
    val background by animateColorAsState(
        when {
            highlighted -> accent.copy(alpha = 0.12f).compositeOver(p.surface)
            isNew -> accent.copy(alpha = 0.05f).compositeOver(p.surface)
            else -> p.surface
        },
        tween(300),
        label = "tile",
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(14.dp)
    // A mini card per comment, with a coloured depth rail on the start edge.
    Column(
        Modifier
            .padding(start = (clamped * 12).dp, bottom = (8 * density).dp)
            .fillMaxWidth()
            // A hardware (outline) shadow: a blurred one per comment cost frames while scrolling.
            .shadow(1.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
            .background(background)
            .drawBehind {
                val w = (if (isMine) 4 else 3).dp.toPx()
                drawRect(railColor, topLeft = Offset(if (rtl) size.width - w else 0f, 0f), size = Size(w, size.height))
            }
            .padding(start = 14.dp, end = 10.dp, top = (9 * density).dp, bottom = (9 * density).dp),
    ) {
        if (isNew) {
            Text(l.commentNew, style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = accent))
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBubble(author, 26.dp, Modifier.clip(CircleShape).clickable(onClick = onOpenUser))
            Spacer(Modifier.width(9.dp))
            Text(
                author,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontWeight = FontWeight.W800, color = p.ink, fontSize = 14.sp),
                modifier = Modifier.weight(1f, fill = false).clickable(onClick = onOpenUser),
            )
            if (isOp) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 1.5.dp),
                ) {
                    Text(l.op, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = accent))
                }
            }
            Spacer(Modifier.weight(1f))
            if (message.points != null) {
                PointsChip(message.points, p.muted, l.memberPoints, onOpenUser)
                Spacer(Modifier.width(8.dp))
            }
            if (message.timestamp != null) {
                Text(relTime(message.timestamp, l, lang), style = TextStyle(fontSize = 11.5.sp, color = muted))
            } else if (message.time != null) {
                Text(message.time, style = TextStyle(fontSize = 11.5.sp, color = muted))
            }
        }
        // Whom this answers — and a way back to them: the parent has usually
        // scrolled away by the time you read the reply.
        if (row.parentAuthor != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onJumpToParent)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Reply, null, tint = accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        l.replyingTo(row.parentAuthor.trim()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontSize = 12.sp, color = accent),
                    )
                }
                // Past the cap every level is drawn alike; say how deep this is.
                if (depth > MAX_INDENT) {
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier
                            .background(p.field, RoundedCornerShape(999.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SubdirectoryArrowLeft, l.depthLevel(depth), tint = muted, modifier = Modifier.size(11.dp))
                        Text("$depth", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W800, color = muted))
                    }
                }
            }
        }
        val title = message.title
        if (!title.isNullOrEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text(
                title,
                style = TextStyle(
                    fontWeight = FontWeight.W700,
                    fontSize = 14.5.sp,
                    color = p.ink,
                    lineHeight = 1.3.em,
                    textDirection = TextDirection.Rtl,
                ),
            )
        }
        if (!collapsed && !message.bodyHtml.isNullOrEmpty()) {
            Spacer(Modifier.height(6.dp))
            PostBody(message.bodyHtml, baseUrl, fontSize = 14.5f)
        } else if (!collapsed && title.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(l.titleOnly, style = TextStyle(fontSize = 12.5.sp, fontStyle = FontStyle.Italic, color = muted))
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasKids) {
                CompactTextButton(
                    onToggleCollapse,
                    accent,
                    null,
                    if (collapsed) l.showReplies(row.childCount) else l.hideReplies,
                    bold = true,
                )
            }
            Spacer(Modifier.weight(1f))
            // Only your own messages can be edited.
            if (isMine) {
                CompactTextButton({
                    view.selectionClick()
                    onEdit()
                }, muted, Icons.Rounded.Edit, l.edit)
            }
            // Enabled when signed out too — it signs in first.
            CompactTextButton({
                view.selectionClick()
                onReply()
            }, muted, Icons.AutoMirrored.Rounded.Reply, l.reply)
        }
    }
}
