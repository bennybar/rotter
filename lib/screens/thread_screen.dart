import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scrollable_positioned_list/scrollable_positioned_list.dart';

import '../l10n/app_localizations.dart';
import '../models/message.dart';
import '../models/scoop.dart';
import '../nav.dart';
import '../services/auth_service.dart';
import '../services/read_store.dart';
import '../services/rotter_service.dart';
import '../services/settings_controller.dart';
import '../theme.dart';
import '../util/rel_time.dart';
import '../widgets/post_body.dart';
import '../widgets/scroll_hiding_scaffold.dart';
import 'compose_screen.dart';

class ThreadScreen extends StatefulWidget {
  final Scoop scoop;
  const ThreadScreen({super.key, required this.scoop});

  @override
  State<ThreadScreen> createState() => _ThreadScreenState();
}

class _ThreadScreenState extends State<ThreadScreen> {
  Thread? _thread;
  Object? _error;
  bool _loading = true;
  // Index-addressable scrolling so we can jump to a specific comment row.
  final _itemScroll = ItemScrollController();
  final _itemPositions = ItemPositionsListener.create();
  // The current depth-first flattened rows (kept so the jump buttons can locate
  // the next top-level comment). List index 0 is the root card; row i is at
  // list index i + 1.
  List<({Message msg, int depth, int childCount, bool collapsed})> _flat = const [];
  final _collapsed = <int>{};

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _toggleCollapse(int num) {
    HapticFeedback.selectionClick();
    setState(() {
      _collapsed.contains(num) ? _collapsed.remove(num) : _collapsed.add(num);
    });
  }

  void _scrollTo(int index) {
    if (!_itemScroll.isAttached) return;
    _itemScroll.scrollTo(
      index: index,
      duration: const Duration(milliseconds: 360),
      curve: Curves.easeOutCubic,
    );
  }

  void _jumpToNewest() {
    HapticFeedback.selectionClick();
    _scrollTo(_flat.length); // last row (index 0 is the root card)
  }

  /// Scroll to the next **top-level** comment (depth 0) below the current top of
  /// the viewport, wrapping to the first when past the last.
  void _jumpNextTopLevel() {
    if (_flat.isEmpty) return;
    HapticFeedback.selectionClick();

    // The topmost row actually on screen (ignore SPL's off-viewport cache).
    final onScreen = _itemPositions.itemPositions.value
        .where((p) => p.itemTrailingEdge > 0 && p.itemLeadingEdge < 1);
    final topIndex = onScreen.isEmpty
        ? 0
        : onScreen.map((p) => p.index).reduce((a, b) => a < b ? a : b);

    int? target;
    for (var i = 0; i < _flat.length; i++) {
      if (_flat[i].depth != 0) continue; // skip nested replies
      if (i + 1 > topIndex) {
        target = i + 1;
        break;
      }
    }
    target ??= 1; // wrap to the first top-level comment (row 0 → list index 1)
    _scrollTo(target);
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
      final thread = await RotterService.instance.fetchThread(widget.scoop.id);
      // Opening a thread marks it read at its current reply count, so the list
      // dulls it and only re-highlights once newer comments arrive.
      await ReadStore.instance.markRead(widget.scoop.id, thread.comments.length);
      if (!mounted) return;
      setState(() {
        _thread = thread;
        _loading = false;
      });
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
  /// nodes and carrying depth + child count for each visible row.
  List<({Message msg, int depth, int childCount, bool collapsed})> _flatten(Thread t) {
    final out = <({Message msg, int depth, int childCount, bool collapsed})>[];
    void walk(int parentNum, int depth) {
      for (final child in t.childrenOf(parentNum)) {
        final kids = t.childrenOf(child.num).length;
        final collapsed = _collapsed.contains(child.num);
        out.add((msg: child, depth: depth, childCount: kids, collapsed: collapsed));
        if (!collapsed) walk(child.num, depth + 1);
      }
    }

    walk(0, 0);
    return out;
  }

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
    final posted = await Navigator.of(context).push<bool>(modernRoute(
      ComposeScreen(threadId: widget.scoop.id, parentNum: parentNum),
    ));
    // Re-fetch so a just-posted reply appears and the read baseline advances.
    if (mounted && posted == true) await _refresh();
  }

  /// Edit one of the user's own messages ([num] 0 = the root post). The composer
  /// loads the current text off rotter's edit form.
  Future<void> _edit(int num) async {
    final saved = await Navigator.of(context).push<bool>(modernRoute(
      ComposeScreen(threadId: widget.scoop.id, editNum: num),
    ));
    if (mounted && saved == true) await _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    return ScrollHidingScaffold(
      bar: AppBar(primary: false, title: Text(l.tabScoops)),
      floatingActionButton: _fabs(l),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: RefreshIndicator(
          onRefresh: _refresh,
          child: _body(l),
        ),
      ),
    );
  }

  Widget? _fabs(L10n l) {
    if (!(_thread?.comments.isNotEmpty ?? false)) return null;
    // Locate the user's own reply (first comment authored by them) so we can
    // offer a jump-to-it button next to the navigation FABs.
    final me = AuthService.instance.username.value;
    int? myRow;
    if (me != null && me.isNotEmpty) {
      for (var i = 0; i < _flat.length; i++) {
        if (_flat[i].msg.author == me) {
          myRow = i + 1; // +1 for the root card at index 0
          break;
        }
      }
    }
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (myRow != null) ...[
          FloatingActionButton.small(
            heroTag: 'myReply',
            tooltip: l.myReply,
            backgroundColor: Colors.green.shade600,
            foregroundColor: Colors.white,
            onPressed: () => _scrollTo(myRow!),
            child: const Icon(Icons.reply_rounded),
          ),
          const SizedBox(height: 12),
        ],
        FloatingActionButton.small(
          heroTag: 'nextTopComment',
          tooltip: l.nextComment,
          onPressed: _jumpNextTopLevel,
          child: const Icon(Icons.keyboard_arrow_down_rounded),
        ),
        const SizedBox(height: 12),
        FloatingActionButton.small(
          heroTag: 'jumpNewest',
          tooltip: l.jumpToNewest,
          onPressed: _jumpToNewest,
          child: const Icon(Icons.south_rounded),
        ),
      ],
    );
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
      final is404 = _error.toString().contains('404');
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
    final flat = _flatten(thread);
    _flat = flat; // cache for the jump-to-next-comment buttons
    return ValueListenableBuilder<double>(
      valueListenable: SettingsController.instance.threadDensity,
      builder: (context, density, _) => ScrollablePositionedList.builder(
        itemScrollController: _itemScroll,
        itemPositionsListener: _itemPositions,
        // Top inset clears the overlaid (hideable) app bar; extra bottom
        // clearance so the FAB stack doesn't cover the last post.
        padding: const EdgeInsets.fromLTRB(14, 8 + ScrollHidingScaffold.barHeight, 14, 120),
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
            message: row.msg,
            depth: row.depth,
            childCount: row.childCount,
            collapsed: row.collapsed,
            isOp: root != null && row.msg.author == root.author,
            isMine: me != null && me.isNotEmpty && row.msg.author == me,
            density: density,
            baseUrl: widget.scoop.url,
            onReply: () => _reply(row.msg.num),
            onEdit: () => _edit(row.msg.num),
            onToggleCollapse: () => _toggleCollapse(row.msg.num),
            onOpenUser: () => _openUserProfile(row.msg),
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
              GestureDetector(onTap: onOpenUser, child: _avatarBubble(root!.author, 34)),
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
                        _pointsChip(context, root!.points!, onOpenUser),
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
            // Disabled (greyed) until signed in.
            ValueListenableBuilder<bool>(
              valueListenable: AuthService.instance.loggedIn,
              builder: (context, loggedIn, _) => FilledButton.tonalIcon(
                onPressed: loggedIn ? onReply : null,
                icon: const Icon(Icons.reply_rounded, size: 18),
                label: Text(l.reply),
              ),
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

/// A small, balanced palette so every author gets a stable identity colour for
/// their avatar (works on both the light/cream and dark backgrounds).
const List<Color> _avatarPalette = [
  Color(0xFF3B82F6), // blue
  Color(0xFF10B981), // emerald
  Color(0xFFEC4899), // pink
  Color(0xFF8B5CF6), // violet
  Color(0xFFF59E0B), // amber
  Color(0xFF06B6D4), // cyan
  Color(0xFFEF6C4D), // coral
  Color(0xFF64748B), // slate
];

Color _avatarColor(String name) => _avatarPalette[name.hashCode.abs() % _avatarPalette.length];

/// A round, per-author identity bubble showing the first letter.
Widget _avatarBubble(String name, double size) {
  final c = _avatarColor(name);
  final t = name.trim();
  return Container(
    width: size,
    height: size,
    alignment: Alignment.center,
    decoration: BoxDecoration(color: c.withValues(alpha: 0.16), shape: BoxShape.circle),
    child: Text(t.isEmpty ? '?' : t.substring(0, 1),
        style: TextStyle(color: c, fontWeight: FontWeight.w800, fontSize: size * 0.5)),
  );
}

/// Reputation-points pill (red when negative). Tapping opens the member profile.
Widget _pointsChip(BuildContext context, int pts, VoidCallback? onTap) {
  final c = pts < 0 ? Colors.red.shade400 : cMuted(context);
  return Tooltip(
    message: L10n.of(context)!.memberPoints,
    child: GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
        decoration:
            BoxDecoration(color: c.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(999)),
        // LTR so a negative renders as "-3", not "3-" in the RTL thread.
        child: Text('$pts',
            textDirection: TextDirection.ltr,
            style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: c)),
      ),
    ),
  );
}

class _CommentTile extends StatelessWidget {
  final Message message;
  final int depth;
  final int childCount;
  final bool collapsed;
  final bool isOp;
  final bool isMine;
  final double density;
  final String baseUrl;
  final VoidCallback onReply;
  final VoidCallback onEdit;
  final VoidCallback onToggleCollapse;
  final VoidCallback onOpenUser;

  const _CommentTile({
    required this.message,
    required this.depth,
    required this.childCount,
    required this.collapsed,
    required this.isOp,
    required this.isMine,
    required this.density,
    required this.baseUrl,
    required this.onReply,
    required this.onEdit,
    required this.onToggleCollapse,
    required this.onOpenUser,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final clamped = depth.clamp(0, 6);
    final hasKids = childCount > 0;
    final railColor = isMine
        ? Colors.green.shade600
        : (depth == 0 ? accent : accent.withValues(alpha: 0.45));
    final author = message.author.trim();

    // Flat over the base (no card). A rounded inset rail on the start edge marks
    // each comment + nesting; the avatar gives every author an identity colour.
    final body = Padding(
      padding: EdgeInsetsDirectional.fromSTEB(14, 9 * density, 10, 9 * density),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            GestureDetector(onTap: onOpenUser, child: _avatarBubble(author, 26)),
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
              _pointsChip(context, message.points!, onOpenUser),
              const SizedBox(width: 8),
            ],
            if (message.timestamp != null)
              Text(relTime(message.timestamp!, context),
                  style: TextStyle(fontSize: 11.5, color: cMuted(context)))
            else if (message.time != null)
              Text(message.time!, style: TextStyle(fontSize: 11.5, color: cMuted(context))),
          ]),
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
                style: TextStyle(
                    fontSize: 12.5, fontStyle: FontStyle.italic, color: cMuted(context))),
          ],
          const SizedBox(height: 2),
          Row(children: [
            if (hasKids)
              TextButton.icon(
                style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact, foregroundColor: cMuted(context)),
                onPressed: onToggleCollapse,
                icon: Icon(
                    collapsed ? Icons.unfold_more_rounded : Icons.unfold_less_rounded, size: 16),
                label: Text(collapsed ? '$childCount' : '', style: const TextStyle(fontSize: 12.5)),
              ),
            const Spacer(),
            // Only your own messages can be edited.
            if (isMine)
              TextButton.icon(
                style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact, foregroundColor: cMuted(context)),
                onPressed: () {
                  HapticFeedback.selectionClick();
                  onEdit();
                },
                icon: const Icon(Icons.edit_rounded, size: 16),
                label: Text(l.edit, style: const TextStyle(fontSize: 12.5)),
              ),
            ValueListenableBuilder<bool>(
              valueListenable: AuthService.instance.loggedIn,
              builder: (context, loggedIn, _) => TextButton.icon(
                style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact, foregroundColor: cMuted(context)),
                onPressed: loggedIn
                    ? () {
                        HapticFeedback.selectionClick();
                        onReply();
                      }
                    : null,
                icon: const Icon(Icons.reply_rounded, size: 16),
                label: Text(l.reply, style: const TextStyle(fontSize: 12.5)),
              ),
            ),
          ]),
        ],
      ),
    );

    // A mini card per comment. The coloured depth edge is Positioned (not a
    // Border / IntrinsicHeight Row) so it stretches to the card height with no
    // extra layout pass — the unpositioned `body` sizes the Stack.
    return RepaintBoundary(
      child: Container(
        margin: EdgeInsetsDirectional.only(start: clamped * 12.0, bottom: 8 * density),
        decoration: BoxDecoration(
          color: cSurface(context),
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
