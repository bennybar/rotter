import 'dart:async';
import 'dart:isolate';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:html/parser.dart' as html_parser;
import 'package:scrollable_positioned_list/scrollable_positioned_list.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../l10n/app_localizations.dart';
import '../models/message.dart';
import '../models/scoop.dart';
import '../nav.dart';
import '../services/ai_store.dart';
import '../services/auth_service.dart';
import '../services/read_store.dart';
import '../services/reading_store.dart';
import '../services/rotter_service.dart';
import '../services/saved_store.dart';
import '../services/settings_controller.dart';
import '../theme.dart';
import '../util/rel_time.dart';
import '../widgets/member_chips.dart';
import '../widgets/post_body.dart';
import '../widgets/scroll_hiding_scaffold.dart';
import 'compose_screen.dart';
import 'summary_screen.dart';

/// One visible row of the flattened comment tree.
typedef _Row = ({
  Message msg,
  int depth,
  int childCount,
  bool collapsed,
  // Who this answers, when that's another commenter (null for replies to the
  // root). Past the indent cap it's the only cue tying a reply to its parent.
  String? parentAuthor,
});

class ThreadScreen extends StatefulWidget {
  final Scoop scoop;
  const ThreadScreen({super.key, required this.scoop});

  @override
  State<ThreadScreen> createState() => _ThreadScreenState();
}

class _ThreadScreenState extends State<ThreadScreen> {
  static const _hintKey = 'threadNavigationExplained';

  Thread? _thread;
  Object? _error;
  bool _loading = true;
  // Index-addressable scrolling so we can jump to a specific comment row.
  final _itemScroll = ItemScrollController();
  final _itemPositions = ItemPositionsListener.create();
  // The current depth-first flattened rows. List index 0 is the root card;
  // row i is at list index i + 1.
  List<_Row> _flat = const [];
  final _collapsed = <int>{};

  /// Highest message number seen on the previous visit, fixed for this screen's
  /// life so a refresh after posting doesn't wipe the "new" markers.
  int? _visitBaseline;
  Set<int> _newNums = const {};
  int? _lastNew; // the new comment last jumped to, for "i of n"

  /// Where the reader is (message number, 0 = the root): updated when a scroll
  /// settles and on every jump, and remembered across visits.
  int? _position;
  int? _resumeTarget; // offered as "Resume reading" on open
  bool _restored = false;

  /// Comments jumped away from via "replying to", for "Back to reply".
  final _returnStack = <int>[];
  int? _highlighted;
  Timer? _highlightTimer;

  bool _searching = false;
  final _searchCtrl = TextEditingController();
  Timer? _searchDebounce;
  List<(int, String)>? _searchIndex; // (num, lowercase text) in display order
  List<int> _matches = const [];
  int _matchIndex = 0;
  bool _searchPending = false;

  bool _hintDismissed = true;

  @override
  void initState() {
    super.initState();
    _load();
    SharedPreferences.getInstance().then((p) {
      if (mounted) setState(() => _hintDismissed = p.getBool(_hintKey) ?? false);
    });
  }

  @override
  void dispose() {
    _highlightTimer?.cancel();
    _searchDebounce?.cancel();
    _searchCtrl.dispose();
    super.dispose();
  }

  void _toggleCollapse(int num) {
    HapticFeedback.selectionClick();
    setState(() {
      _collapsed.contains(num) ? _collapsed.remove(num) : _collapsed.add(num);
    });
  }

  bool get _allCollapsed =>
      _flat.isNotEmpty && _flat.every((r) => r.depth == 0) && _flat.any((r) => r.collapsed);

  void _toggleCollapseAll() {
    final t = _thread;
    if (t == null) return;
    HapticFeedback.selectionClick();
    setState(() {
      if (_allCollapsed) {
        _collapsed.clear();
      } else {
        _collapsed
          ..clear()
          ..addAll(t.childrenOf(0).map((m) => m.num));
      }
    });
  }

  int _indexOf(int num) {
    if (num == 0) return 0;
    final i = _flat.indexWhere((r) => r.msg.num == num);
    return i < 0 ? -1 : i + 1;
  }

  /// Nested replies can be inside collapsed branches: open their ancestors.
  void _reveal(int num) {
    final byNum = {for (final m in _thread!.messages) m.num: m};
    var cur = num;
    final seen = <int>{};
    while (seen.add(cur)) {
      _collapsed.remove(cur);
      final parent = byNum[cur]?.parent;
      if (parent == null || parent == 0) break;
      cur = parent;
    }
  }

  /// Scroll to message [num] and tint it briefly, so the reader can see where
  /// they landed instead of working it out from the surrounding text.
  void _jump(int num) {
    final t = _thread;
    if (t == null || !t.messages.any((m) => m.num == num)) return;
    HapticFeedback.selectionClick();
    setState(() {
      _reveal(num);
      _resumeTarget = null;
      if (_newNums.contains(num)) _lastNew = num;
      _position = num;
      _highlighted = num;
    });
    ReadingStore.instance.rememberPosition(widget.scoop.id, num);
    _highlightTimer?.cancel();
    _highlightTimer = Timer(const Duration(milliseconds: 1400), () {
      if (mounted) setState(() => _highlighted = null);
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final index = _indexOf(num);
      if (index < 0 || !_itemScroll.isAttached) return;
      _itemScroll.scrollTo(
        index: index,
        // Land just below the overlaid app bar, not under it.
        alignment: (ScrollHidingScaffold.barHeight + 8) / MediaQuery.sizeOf(context).height,
        duration: const Duration(milliseconds: 360),
        curve: Curves.easeOutCubic,
      );
    });
  }

  /// The message at the top of the viewport (0 = the root card).
  int _topVisibleNum() {
    final top = (ScrollHidingScaffold.barHeight + 8) / MediaQuery.sizeOf(context).height;
    final onScreen = _itemPositions.itemPositions.value.where((p) => p.itemTrailingEdge > top);
    if (onScreen.isEmpty) return 0;
    final index = onScreen.map((p) => p.index).reduce((a, b) => a < b ? a : b);
    return index == 0 || index > _flat.length ? 0 : _flat[index - 1].msg.num;
  }

  bool _onScroll(ScrollNotification n) {
    if (n is ScrollStartNotification && n.dragDetails != null) {
      // The reader took over: drop the offers and the landing tint.
      if (_resumeTarget != null || _highlighted != null) {
        setState(() {
          _resumeTarget = null;
          _highlighted = null;
        });
      }
      _userScrolling = true;
    } else if (n is ScrollEndNotification && _userScrolling) {
      _userScrolling = false;
      _position = _topVisibleNum();
      ReadingStore.instance.rememberPosition(widget.scoop.id, _position!);
      setState(() {}); // the jump buttons read the position
    }
    return false;
  }

  bool _userScrolling = false;

  /// The next top-level comment below the current position — stopping at the
  /// end rather than unexpectedly wrapping back to the top.
  int? _nextDiscussion() {
    final pos = _position ?? _topVisibleNum();
    final start = pos == 0 ? 0 : _flat.indexWhere((r) => r.msg.num == pos) + 1;
    for (var i = start; i < _flat.length; i++) {
      if (_flat[i].depth == 0) return _flat[i].msg.num;
    }
    return null;
  }

  /// Loads the thread, catching errors into state so they're shown in-UI rather
  /// than surfacing as an unhandled future error (a fresh thread's `.shtml`
  /// snapshot can 404 until rotter generates it).
  Future<void> _load() async {
    // _loading guards the spinner only while _thread is null, so setting it on a
    // retry (after an error, _thread still null) avoids briefly hitting `_thread!`.
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final id = widget.scoop.id;
      final thread = await RotterService.instance.fetchThread(id);
      final latest = thread.comments.fold<int>(0, (a, m) => m.num > a ? m.num : a);
      // A first-ever visit sets a baseline; it doesn't label everything new.
      _visitBaseline ??= ReadingStore.instance.latestMessage(id) ?? latest;
      final baseline = _visitBaseline!;
      await ReadingStore.instance.recordVisit(id, latest);
      // Opening a thread marks it read at its current reply count, so the list
      // dulls it and only re-highlights once newer comments arrive.
      await ReadStore.instance.markRead(id, thread.comments.length);
      if (!mounted) return;
      setState(() {
        _thread = thread;
        _loading = false;
        _newNums = {for (final m in thread.comments) if (m.num > baseline) m.num};
        _searchIndex = null;
        if (!_restored) {
          _restored = true;
          final saved = ReadingStore.instance.position(id);
          if (saved != null && saved != 0) {
            final nums = thread.comments.map((m) => m.num).toList()..sort();
            _resumeTarget = nums.contains(saved)
                ? saved
                : nums.cast<int?>().firstWhere((n) => n! > saved, orElse: () => null);
          }
        }
      });
      if (_searching) _runSearch(_searchCtrl.text);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e;
        _loading = false;
      });
    }
  }

  Future<void> _refresh() => _load();

  /// Depth-first flatten of the comment tree, skipping descendants of collapsed
  /// nodes (unless [all]) and carrying what each row needs.
  List<_Row> _flatten(Thread t, {bool all = false}) {
    final authors = {for (final m in t.messages) m.num: m.author};
    final out = <_Row>[];
    void walk(int parentNum, int depth) {
      for (final child in t.childrenOf(parentNum)) {
        final collapsed = _collapsed.contains(child.num);
        final p = child.parent ?? 0;
        out.add((
          msg: child,
          depth: depth,
          childCount: t.childrenOf(child.num).length,
          collapsed: collapsed,
          parentAuthor: p == 0 ? null : authors[p],
        ));
        if (all || !collapsed) walk(child.num, depth + 1);
      }
    }

    walk(0, 0);
    return out;
  }

  // ---- Search -----------------------------------------------------------------

  void _openSearch() => setState(() => _searching = true);

  void _closeSearch() {
    _searchDebounce?.cancel();
    _searchCtrl.clear();
    setState(() {
      _searching = false;
      _matches = const [];
      _searchPending = false;
    });
  }

  void _onSearchChanged(String q) {
    _searchDebounce?.cancel();
    setState(() {
      _searchPending = q.trim().isNotEmpty;
      _matches = const [];
    });
    _searchDebounce = Timer(const Duration(milliseconds: 250), () => _runSearch(q));
  }

  /// Author, title and body text, in reading order. The plain-text index is
  /// built once per load off the UI isolate — bodies are HTML.
  Future<void> _runSearch(String raw) async {
    final q = raw.trim().toLowerCase();
    final t = _thread;
    if (q.isEmpty || t == null) {
      setState(() {
        _matches = const [];
        _searchPending = false;
      });
      return;
    }
    if (_searchIndex == null) {
      final ordered = [if (t.root != null) t.root!, ..._flatten(t, all: true).map((r) => r.msg)];
      final input = [for (final m in ordered) (m.num, m.author, m.title ?? '', m.bodyHtml ?? '')];
      _searchIndex = await Isolate.run(() => [
            for (final (n, a, title, html) in input)
              (n, '$a\n$title\n${html_parser.parseFragment(html).text ?? ''}'.toLowerCase()),
          ]);
    }
    if (!mounted || _searchCtrl.text.trim().toLowerCase() != q) return;
    final matches = [for (final (n, text) in _searchIndex!) if (text.contains(q)) n];
    setState(() {
      _matches = matches;
      _matchIndex = 0;
      _searchPending = false;
    });
    if (matches.isNotEmpty) _jump(matches.first);
  }

  void _stepMatch(int delta) {
    final i = _matchIndex + delta;
    if (i < 0 || i >= _matches.length) return;
    setState(() => _matchIndex = i);
    _jump(_matches[i]);
  }

  // ---- Actions ----------------------------------------------------------------

  void _openUserProfile(Message m) {
    HapticFeedback.selectionClick();
    final posts =
        _thread?.messages.where((x) => x.author == m.author).toList() ?? [m];
    // The member stats repeat on every post by that author; use whichever carries them.
    final stats = posts.firstWhere((p) => p.points != null, orElse: () => m);
    Navigator.of(context).push(modernRoute(
      UserProfileScreen(
        name: m.author,
        joinDate: stats.joinDate,
        messages: stats.messages,
        raters: stats.raters,
        points: stats.points,
        posts: posts,
        baseUrl: widget.scoop.url,
      ),
    ));
  }

  Future<void> _reply(int parentNum) async {
    final posted = await openComposer(
        context, ComposeScreen(threadId: widget.scoop.id, parentNum: parentNum));
    // Re-fetch so a just-posted reply appears and the read baseline advances.
    if (mounted && posted == true) await _refresh();
  }

  /// Edit one of the user's own messages ([num] 0 = the root post). The composer
  /// loads the current text off rotter's edit form.
  Future<void> _edit(int num) async {
    final saved = await openComposer(
        context, ComposeScreen(threadId: widget.scoop.id, editNum: num));
    if (mounted && saved == true) await _refresh();
  }

  void _summarize() {
    final t = _thread;
    if (t == null) return;
    Navigator.of(context).push(modernRoute(
        SummaryScreen(thread: t, title: t.root?.title ?? widget.scoop.title)));
  }

  Future<void> _dismissHint() async {
    setState(() => _hintDismissed = true);
    await (await SharedPreferences.getInstance()).setBool(_hintKey, true);
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final chrome = Directionality.of(context);
    if (_thread != null) _flat = _flatten(_thread!);
    return ScrollHidingScaffold(
      pinned: _searching,
      bar: _searching ? _searchBar(l) : _titleBar(l),
      floatingActionButton: _fabs(l),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: Stack(children: [
          Positioned.fill(
            child: RefreshIndicator(
              onRefresh: _refresh,
              child: NotificationListener<ScrollNotification>(
                onNotification: _onScroll,
                child: _body(l),
              ),
            ),
          ),
          if (_resumeTarget != null)
            Positioned(
              top: ScrollHidingScaffold.barHeight + 6,
              left: 12,
              right: 12,
              child: Directionality(textDirection: chrome, child: _resumeBanner(l)),
            ),
        ]),
      ),
    );
  }

  PreferredSizeWidget _titleBar(L10n l) {
    final t = _thread;
    return AppBar(
      primary: false,
      title: Text(l.tabScoops),
      actions: [
        if (t != null)
          IconButton(
            tooltip: l.searchThread,
            icon: const Icon(Icons.search_rounded),
            onPressed: _openSearch,
          ),
        // Only when summaries are on and a key is stored — otherwise the button
        // would promise something the app can't do.
        ListenableBuilder(
          listenable: AIStore.instance,
          builder: (context, _) => t != null && AIStore.instance.isReady
              ? IconButton(
                  tooltip: l.aiSummarize,
                  icon: const Icon(Icons.auto_awesome_rounded),
                  onPressed: _summarize,
                )
              : const SizedBox.shrink(),
        ),
        ListenableBuilder(
          listenable: SavedStore.saved,
          builder: (context, _) {
            final saved = SavedStore.saved.contains(widget.scoop.id);
            return IconButton(
              tooltip: saved ? l.unsaveScoop : l.saveScoop,
              icon: Icon(saved ? Icons.bookmark_rounded : Icons.bookmark_outline_rounded),
              onPressed: () {
                HapticFeedback.selectionClick();
                SavedStore.saved.toggle(widget.scoop);
              },
            );
          },
        ),
        if (t != null)
          PopupMenuButton<String>(
            onSelected: (v) {
              if (v == 'follow') SavedStore.followed.toggle(widget.scoop);
              if (v == 'collapse') _toggleCollapseAll();
            },
            itemBuilder: (_) {
              final following = SavedStore.followed.contains(widget.scoop.id);
              return [
                PopupMenuItem(
                  value: 'follow',
                  child: Row(children: [
                    Icon(following
                        ? Icons.notifications_off_outlined
                        : Icons.notifications_none_rounded),
                    const SizedBox(width: 10),
                    Text(following ? l.unfollow : l.follow),
                  ]),
                ),
                PopupMenuItem(
                  value: 'collapse',
                  enabled: t.comments.isNotEmpty,
                  child: Row(children: [
                    Icon(_allCollapsed ? Icons.unfold_more_rounded : Icons.unfold_less_rounded),
                    const SizedBox(width: 10),
                    Text(_allCollapsed ? l.expandAll : l.collapseAll),
                  ]),
                ),
              ];
            },
          ),
      ],
    );
  }

  PreferredSizeWidget _searchBar(L10n l) {
    final hasQuery = _searchCtrl.text.trim().isNotEmpty;
    return AppBar(
      primary: false,
      automaticallyImplyLeading: false,
      titleSpacing: 12,
      title: TextField(
        controller: _searchCtrl,
        autofocus: true,
        textInputAction: TextInputAction.search,
        onChanged: _onSearchChanged,
        decoration: InputDecoration(
          hintText: l.searchThreadHint,
          isDense: true,
          filled: true,
          fillColor: cField(context),
          border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        ),
      ),
      actions: [
        if (_searchPending)
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 8),
            child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
          )
        else if (hasQuery)
          Text(
            _matches.isEmpty ? l.matchesCount(0) : '${_matchIndex + 1} / ${_matches.length}',
            textDirection: TextDirection.ltr,
            style: TextStyle(fontSize: 12.5, color: cMuted(context)),
          ),
        IconButton(
          tooltip: l.previousMatch,
          icon: const Icon(Icons.keyboard_arrow_up_rounded),
          onPressed: _searchPending || _matchIndex == 0 ? null : () => _stepMatch(-1),
        ),
        IconButton(
          tooltip: l.nextMatch,
          icon: const Icon(Icons.keyboard_arrow_down_rounded),
          onPressed: _searchPending || _matchIndex + 1 >= _matches.length
              ? null
              : () => _stepMatch(1),
        ),
        IconButton(
          tooltip: l.close,
          icon: const Icon(Icons.close_rounded),
          onPressed: _closeSearch,
        ),
      ],
    );
  }

  Widget _resumeBanner(L10n l) => Material(
        color: cSurface(context),
        elevation: 3,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
          child: Row(children: [
            TextButton.icon(
              onPressed: () => _jump(_resumeTarget!),
              icon: const Icon(Icons.bookmark_rounded, size: 18),
              label: Text(l.resumeReading),
            ),
            const Spacer(),
            TextButton(
              style: TextButton.styleFrom(foregroundColor: cMuted(context)),
              onPressed: () => setState(() => _resumeTarget = null),
              child: Text(l.startTop),
            ),
          ]),
        ),
      );

  Widget? _fabs(L10n l) {
    final t = _thread;
    if (t == null || t.comments.isEmpty) return null;
    final me = AuthService.instance.username.value;
    final myNum = me == null || me.isEmpty
        ? null
        : t.comments.cast<Message?>().firstWhere((m) => m!.author == me, orElse: () => null)?.num;
    // Message numbers grow as replies are posted, whatever their tree position.
    final newest = t.comments.fold<int>(0, (a, m) => m.num > a ? m.num : a);
    final next = _nextDiscussion();
    final news = _newNums.toList()..sort();
    final newIndex = _lastNew == null ? -1 : news.indexOf(_lastNew!);
    final prevNew = _lastNew == null
        ? null
        : news.cast<int?>().lastWhere((n) => n! < _lastNew!, orElse: () => null);
    final nextNew = _lastNew == null
        ? (news.isEmpty ? null : news.first)
        : news.cast<int?>().firstWhere((n) => n! > _lastNew!, orElse: () => null);
    final accent = Theme.of(context).colorScheme.primary;

    Widget pill({required Widget child}) => Material(
          color: cSurface(context),
          elevation: 3,
          shape: const StadiumBorder(),
          child: child,
        );

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        if (!_hintDismissed) ...[
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 250),
            child: Material(
              color: cSurface(context),
              elevation: 3,
              borderRadius: BorderRadius.circular(14),
              child: Padding(
                padding: const EdgeInsetsDirectional.fromSTEB(12, 8, 4, 10),
                child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(l.navigationHint,
                          style: TextStyle(fontSize: 12.5, color: cInk(context), height: 1.4)),
                    ),
                  ),
                  IconButton(
                    tooltip: l.close,
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.close_rounded, size: 18),
                    onPressed: _dismissHint,
                  ),
                ]),
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (_returnStack.isNotEmpty) ...[
          pill(
            child: InkWell(
              customBorder: const StadiumBorder(),
              onTap: () {
                final n = _returnStack.removeLast();
                _jump(n);
              },
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  Icon(Icons.u_turn_left_rounded, size: 18, color: accent),
                  const SizedBox(width: 6),
                  Text(l.backToReply,
                      style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
                ]),
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (news.isNotEmpty) ...[
          pill(
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              IconButton(
                tooltip: l.previousNew,
                icon: const Icon(Icons.keyboard_arrow_up_rounded),
                onPressed: prevNew == null ? null : () => _jumpNew(prevNew),
              ),
              Text(
                newIndex >= 0 ? l.newOfCount(newIndex + 1, news.length) : l.newRepliesCount(news.length),
                style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: accent),
              ),
              IconButton(
                tooltip: l.nextNew,
                icon: const Icon(Icons.keyboard_arrow_down_rounded),
                onPressed: nextNew == null ? null : () => _jumpNew(nextNew),
              ),
            ]),
          ),
          const SizedBox(height: 12),
        ],
        if (myNum != null) ...[
          FloatingActionButton.small(
            heroTag: 'myReply',
            tooltip: l.myReply,
            backgroundColor: Colors.green.shade600,
            foregroundColor: Colors.white,
            onPressed: () => _jumpFab(myNum),
            child: const Icon(Icons.reply_rounded),
          ),
          const SizedBox(height: 12),
        ],
        FloatingActionButton.small(
          heroTag: 'nextTopComment',
          tooltip: l.nextComment,
          // Disabled at the last discussion rather than wrapping to the top.
          backgroundColor: next == null ? cField(context) : null,
          foregroundColor: next == null ? cMuted(context) : null,
          onPressed: next == null ? null : () => _jumpFab(next),
          child: const Icon(Icons.keyboard_arrow_down_rounded),
        ),
        const SizedBox(height: 12),
        FloatingActionButton.small(
          heroTag: 'jumpNewest',
          tooltip: l.jumpToNewest,
          onPressed: () => _jumpFab(newest),
          child: const Icon(Icons.south_rounded),
        ),
      ],
    );
  }

  void _jumpNew(int num) {
    _returnStack.clear();
    _jump(num);
  }

  void _jumpFab(int num) {
    _returnStack.clear();
    _jump(num);
  }

  Widget _body(L10n l) {
    if (_loading && _thread == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _thread == null) {
      // A 404 is either a brand-new post whose static .shtml isn't generated yet,
      // or a thread that was removed. Tell them apart by age: a fresh post is
      // "not ready, retry"; an older one that 404s was almost certainly removed
      // (it lingers in the RSS feed until that snapshot regenerates).
      final is404 = _error is RotterHttpException && (_error as RotterHttpException).isNotFound;
      final published = widget.scoop.published;
      final fresh = published != null &&
          DateTime.now().difference(published) < const Duration(minutes: 15);
      final removed = is404 && !fresh;
      final notReady = is404 && fresh;
      return ListView(physics: const AlwaysScrollableScrollPhysics(), children: [
        SizedBox(height: MediaQuery.of(context).size.height * 0.28),
        Icon(
            removed
                ? Icons.delete_outline_rounded
                : notReady
                    ? Icons.hourglass_empty_rounded
                    : Icons.cloud_off_rounded,
            size: 46,
            color: cMuted(context)),
        const SizedBox(height: 12),
        Center(
            child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Text(
              removed
                  ? l.scoopRemoved
                  : notReady
                      ? l.threadNotReady
                      : l.loadingError,
              textAlign: TextAlign.center,
              style: TextStyle(color: cMuted(context))),
        )),
        const SizedBox(height: 14),
        // A removed thread won't come back, so only offer retry when it might.
        if (!removed)
          Center(child: FilledButton.tonal(onPressed: _refresh, child: Text(l.retry))),
      ]);
    }
    final thread = _thread!;
    final root = thread.root;
    final me = AuthService.instance.username.value;
    final flat = _flat;
    return ValueListenableBuilder<double>(
      valueListenable: SettingsController.instance.threadDensity,
      builder: (context, density, _) => ScrollablePositionedList.builder(
        itemScrollController: _itemScroll,
        itemPositionsListener: _itemPositions,
        // Top inset clears the overlaid (hideable) app bar; extra bottom
        // clearance so the FAB stack doesn't cover the last post.
        padding: const EdgeInsets.fromLTRB(14, 8 + ScrollHidingScaffold.barHeight, 14, 160),
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: 1 + flat.length,
        itemBuilder: (context, i) {
          if (i == 0) {
            return _RootCard(
              scoop: widget.scoop,
              root: root,
              replyCount: thread.comments.length,
              isMine: me != null && me.isNotEmpty && root?.author == me,
              onReply: () => _reply(0),
              onEdit: () => _edit(0),
              onOpenUser: root == null ? null : () => _openUserProfile(root),
            );
          }
          final row = flat[i - 1];
          return _CommentTile(
            row: row,
            isOp: root != null && row.msg.author == root.author,
            isMine: me != null && me.isNotEmpty && row.msg.author == me,
            isNew: _newNums.contains(row.msg.num),
            highlighted: _highlighted == row.msg.num,
            density: density,
            baseUrl: widget.scoop.url,
            onReply: () => _reply(row.msg.num),
            onEdit: () => _edit(row.msg.num),
            onToggleCollapse: () => _toggleCollapse(row.msg.num),
            onOpenUser: () => _openUserProfile(row.msg),
            onJumpToParent: () {
              final parent = row.msg.parent;
              if (parent == null) return;
              _returnStack.add(row.msg.num);
              _jump(parent);
            },
          );
        },
      ),
    );
  }
}

class _RootCard extends StatelessWidget {
  final Scoop scoop;
  final Message? root;
  final int replyCount;
  final bool isMine;
  final VoidCallback onReply;
  final VoidCallback onEdit;
  final VoidCallback? onOpenUser;

  const _RootCard({
    required this.scoop,
    required this.root,
    required this.replyCount,
    required this.isMine,
    required this.onReply,
    required this.onEdit,
    this.onOpenUser,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.fromLTRB(14, 16, 14, 14),
      decoration: BoxDecoration(
        color: cSurface(context),
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 18,
              offset: const Offset(0, 6)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(root?.title ?? scoop.title,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(height: 1.3)),
          const SizedBox(height: 14),
          Builder(builder: (context) {
            final time = scoop.published != null
                ? relTime(scoop.published!, context)
                : (root?.time ?? '');
            final timeText = Text(time, style: TextStyle(color: cMuted(context), fontSize: 12.5));
            if (root == null) return timeText;
            // Tap avatar/name → native profile.
            return Row(children: [
              GestureDetector(onTap: onOpenUser, child: avatarBubble(root!.author, 34)),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(children: [
                      Flexible(
                        child: GestureDetector(
                          onTap: onOpenUser,
                          child: Text(root!.author,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                  fontWeight: FontWeight.w800, color: cInk(context), fontSize: 15)),
                        ),
                      ),
                      if (root!.points != null) ...[
                        const SizedBox(width: 8),
                        pointsChip(context, root!.points!, onOpenUser),
                      ],
                    ]),
                    const SizedBox(height: 2),
                    timeText,
                  ],
                ),
              ),
            ]);
          }),
          if (root?.bodyHtml != null) ...[
            const SizedBox(height: 14),
            ..._bodyParagraphs(context, root!.bodyHtml!, scoop.url),
          ],
          const SizedBox(height: 14),
          Row(children: [
            Icon(Icons.mode_comment_outlined, size: 15, color: cMuted(context)),
            const SizedBox(width: 6),
            Text(l.replies(replyCount),
                style: TextStyle(fontWeight: FontWeight.w700, color: cMuted(context), fontSize: 13)),
            const Spacer(),
            // Only your own post can be edited.
            if (isMine)
              TextButton.icon(
                style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact, foregroundColor: cMuted(context)),
                onPressed: onEdit,
                icon: const Icon(Icons.edit_rounded, size: 17),
                label: Text(l.edit),
              ),
            const SizedBox(width: 4),
            // Enabled when signed out too — it signs in first.
            FilledButton.tonalIcon(
              onPressed: onReply,
              icon: const Icon(Icons.reply_rounded, size: 18),
              label: Text(l.reply),
            ),
          ]),
        ],
      ),
    );
  }

  /// Render the OP body as paragraphs (split on the single `<br>` rotter leaves
  /// between blocks) separated by a blank line of breathing room.
  List<Widget> _bodyParagraphs(BuildContext context, String html, String baseUrl) {
    final parts = html
        .split(RegExp(r'<br\s*/?>', caseSensitive: false))
        .map((s) => s.trim())
        .where((s) => s.isNotEmpty && s != '&nbsp;')
        .toList();
    if (parts.length < 2) return [PostBody(html: html, baseUrl: baseUrl)];
    final widgets = <Widget>[];
    for (var i = 0; i < parts.length; i++) {
      if (i > 0) widgets.add(const SizedBox(height: 14));
      widgets.add(PostBody(html: parts[i], baseUrl: baseUrl));
    }
    return widgets;
  }
}

class _CommentTile extends StatelessWidget {
  final _Row row;
  final bool isOp;
  final bool isMine;
  final bool isNew; // posted since the previous visit
  final bool highlighted; // just jumped to
  final double density;
  final String baseUrl;
  final VoidCallback onReply;
  final VoidCallback onEdit;
  final VoidCallback onToggleCollapse;
  final VoidCallback onOpenUser;
  final VoidCallback onJumpToParent;

  /// The indent stops growing here — past it a Hebrew line would be squeezed
  /// too narrow — and a depth badge takes over.
  static const _maxIndent = 6;

  const _CommentTile({
    required this.row,
    required this.isOp,
    required this.isMine,
    required this.isNew,
    required this.highlighted,
    required this.density,
    required this.baseUrl,
    required this.onReply,
    required this.onEdit,
    required this.onToggleCollapse,
    required this.onOpenUser,
    required this.onJumpToParent,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final message = row.msg;
    final depth = row.depth;
    final collapsed = row.collapsed;
    final clamped = depth.clamp(0, _maxIndent);
    final hasKids = row.childCount > 0;
    final railColor = isMine
        ? Colors.green.shade600
        : (depth == 0 ? accent : accent.withValues(alpha: 0.45));
    final author = message.author.trim();
    final muted = cMuted(context);
    final small = TextButton.styleFrom(visualDensity: VisualDensity.compact, foregroundColor: muted);

    // Flat over the base (no card). A rounded inset rail on the start edge marks
    // each comment + nesting; the avatar gives every author an identity colour.
    final body = Padding(
      padding: EdgeInsetsDirectional.fromSTEB(14, 9 * density, 10, 9 * density),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (isNew) ...[
            Text(l.commentNew,
                style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: accent)),
            const SizedBox(height: 4),
          ],
          Row(children: [
            GestureDetector(onTap: onOpenUser, child: avatarBubble(author, 26)),
            const SizedBox(width: 9),
            Flexible(
              child: GestureDetector(
                onTap: onOpenUser,
                child: Text(author,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                        fontWeight: FontWeight.w800, color: cInk(context), fontSize: 14)),
              ),
            ),
            if (isOp) ...[const SizedBox(width: 6), _badge(context, l.op, accent)],
            const Spacer(),
            if (message.points != null) ...[
              pointsChip(context, message.points!, onOpenUser),
              const SizedBox(width: 8),
            ],
            if (message.timestamp != null)
              Text(relTime(message.timestamp!, context),
                  style: TextStyle(fontSize: 11.5, color: muted))
            else if (message.time != null)
              Text(message.time!, style: TextStyle(fontSize: 11.5, color: muted)),
          ]),
          // Whom this answers — and a way back to them: the parent has usually
          // scrolled away by the time you read the reply.
          if (row.parentAuthor != null) ...[
            const SizedBox(height: 4),
            Row(children: [
              Flexible(
                child: InkWell(
                  onTap: onJumpToParent,
                  borderRadius: BorderRadius.circular(6),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
                      Icon(Icons.reply_rounded, size: 13, color: accent),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(l.replyingTo(row.parentAuthor!.trim()),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(fontSize: 12, color: accent)),
                      ),
                    ]),
                  ),
                ),
              ),
              // Past the cap every level is drawn alike; say how deep this is.
              if (depth > _maxIndent) ...[
                const SizedBox(width: 6),
                Tooltip(
                  message: l.depthLevel(depth),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                    decoration: BoxDecoration(
                        color: cField(context), borderRadius: BorderRadius.circular(999)),
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
                      Icon(Icons.subdirectory_arrow_left_rounded, size: 11, color: muted),
                      Text('$depth',
                          style: TextStyle(fontSize: 10, fontWeight: FontWeight.w800, color: muted)),
                    ]),
                  ),
                ),
              ],
            ]),
          ],
          if (message.title != null && message.title!.isNotEmpty) ...[
            const SizedBox(height: 7),
            Text(message.title!,
                textDirection: TextDirection.rtl,
                style: TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14.5,
                    color: cInk(context),
                    height: 1.3)),
          ],
          if (!collapsed && message.bodyHtml != null && message.bodyHtml!.isNotEmpty) ...[
            const SizedBox(height: 6),
            PostBody(html: message.bodyHtml!, baseUrl: baseUrl, fontSize: 14.5),
          ] else if (!collapsed && (message.title == null || message.title!.isEmpty)) ...[
            const SizedBox(height: 4),
            Text(l.titleOnly,
                style: TextStyle(fontSize: 12.5, fontStyle: FontStyle.italic, color: muted)),
          ],
          const SizedBox(height: 2),
          Row(children: [
            if (hasKids)
              TextButton(
                style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact, foregroundColor: accent),
                onPressed: onToggleCollapse,
                child: Text(collapsed ? l.showReplies(row.childCount) : l.hideReplies,
                    style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700)),
              ),
            const Spacer(),
            // Only your own messages can be edited.
            if (isMine)
              TextButton.icon(
                style: small,
                onPressed: () {
                  HapticFeedback.selectionClick();
                  onEdit();
                },
                icon: const Icon(Icons.edit_rounded, size: 16),
                label: Text(l.edit, style: const TextStyle(fontSize: 12.5)),
              ),
            // Enabled when signed out too — it signs in first.
            TextButton.icon(
              style: small,
              onPressed: () {
                HapticFeedback.selectionClick();
                onReply();
              },
              icon: const Icon(Icons.reply_rounded, size: 16),
              label: Text(l.reply, style: const TextStyle(fontSize: 12.5)),
            ),
          ]),
        ],
      ),
    );

    final surface = cSurface(context);
    final background = highlighted
        ? Color.alphaBlend(accent.withValues(alpha: 0.12), surface)
        : isNew
            ? Color.alphaBlend(accent.withValues(alpha: 0.05), surface)
            : surface;

    // A mini card per comment. The coloured depth edge is Positioned (not a
    // Border / IntrinsicHeight Row) so it stretches to the card height with no
    // extra layout pass — the unpositioned `body` sizes the Stack.
    return RepaintBoundary(
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        margin: EdgeInsetsDirectional.only(start: clamped * 12.0, bottom: 8 * density),
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(14),
          boxShadow: [
            BoxShadow(
                color: Colors.black.withValues(alpha: 0.05),
                blurRadius: 6,
                offset: const Offset(0, 2)),
          ],
        ),
        clipBehavior: Clip.antiAlias,
        child: Stack(
          children: [
            PositionedDirectional(
              start: 0,
              top: 0,
              bottom: 0,
              child: Container(width: isMine ? 4 : 3, color: railColor),
            ),
            body,
          ],
        ),
      ),
    );
  }

  Widget _badge(BuildContext context, String text, Color accent) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1.5),
        decoration: BoxDecoration(
            color: accent.withValues(alpha: 0.14), borderRadius: BorderRadius.circular(999)),
        child: Text(text,
            style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w800, color: accent)),
      );
}

/// A fully native member profile: name, reputation points + stats, and the
/// member's posts in the current thread. No webview, no rotter.net page.
class UserProfileScreen extends StatelessWidget {
  final String name;
  final String? joinDate;
  final int? messages;
  final int? raters;
  final int? points;
  final List<Message> posts;
  final String baseUrl;
  const UserProfileScreen({
    super.key,
    required this.name,
    this.joinDate,
    this.messages,
    this.raters,
    this.points,
    required this.posts,
    required this.baseUrl,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    return Scaffold(
      appBar: AppBar(title: Text(l.userDetails)),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
          children: [
            Row(children: [
              CircleAvatar(
                radius: 26,
                backgroundColor: accent.withValues(alpha: 0.15),
                child: Icon(Icons.person_rounded, color: accent, size: 30),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(name,
                        style: TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 18, color: cInk(context))),
                  ],
                ),
              ),
            ]),
            const SizedBox(height: 18),
            // Real rotter member stats (parsed from the thread HTML).
            if (points != null || raters != null || messages != null || joinDate != null)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 14),
                decoration: BoxDecoration(
                  color: cSurface(context),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: [
                    if (points != null) _stat(context, '$points', l.memberPoints, accent),
                    if (raters != null) _stat(context, '$raters', l.memberRaters, accent),
                    if (messages != null) _stat(context, '$messages', l.memberPosts, accent),
                  ],
                ),
              ),
            if (joinDate != null) ...[
              const SizedBox(height: 8),
              Text('${l.memberSince} $joinDate',
                  style: TextStyle(fontSize: 12.5, color: cMuted(context))),
            ],
            const SizedBox(height: 22),
            Text(l.userPostsInThread(posts.length),
                style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.6,
                    color: cMuted(context))),
            const SizedBox(height: 10),
            for (final m in posts)
              Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
                decoration: BoxDecoration(
                  color: cSurface(context),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (m.title != null && m.title!.isNotEmpty)
                      Text(m.title!,
                          style: TextStyle(
                              fontWeight: FontWeight.w700, color: cInk(context), fontSize: 14.5)),
                    if (m.bodyHtml != null && m.bodyHtml!.isNotEmpty) ...[
                      if (m.title != null && m.title!.isNotEmpty) const SizedBox(height: 6),
                      PostBody(html: m.bodyHtml!, baseUrl: baseUrl, fontSize: 14),
                    ],
                    if (m.timestamp != null || m.time != null) ...[
                      const SizedBox(height: 8),
                      Text(
                          m.timestamp != null
                              ? relTime(m.timestamp!, context)
                              : (m.time ?? ''),
                          style: TextStyle(fontSize: 11.5, color: cMuted(context))),
                    ],
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _stat(BuildContext context, String value, String label, Color accent) => Expanded(
        child: Column(children: [
          Text(value,
              style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18, color: accent)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(fontSize: 11.5, color: cMuted(context))),
        ]),
      );
}
