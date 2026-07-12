import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../l10n/app_localizations.dart';
import '../models/scoop.dart';
import '../nav.dart';
import '../services/auth_service.dart';
import '../services/my_replies_store.dart';
import '../services/read_store.dart';
import '../services/rotter_service.dart';
import '../services/scoop_meta.dart';
import '../services/settings_controller.dart';
import '../theme.dart';
import '../util/rel_time.dart';
import '../widgets/scroll_hiding_scaffold.dart';
import 'compose_screen.dart';
import 'thread_screen.dart';

enum _Bucket { today, yesterday, earlier }

class ScoopsScreen extends StatefulWidget {
  const ScoopsScreen({super.key});

  @override
  State<ScoopsScreen> createState() => _ScoopsScreenState();
}

class _ScoopsScreenState extends State<ScoopsScreen> {
  late Future<List<Scoop>> _future;
  final _searchCtrl = TextEditingController();
  String _query = '';
  bool _searching = false; // toggles the in-app-bar search field
  List<Scoop> _loaded = const []; // latest resolved list, for "mark all read"

  @override
  void initState() {
    super.initState();
    _future = _load();
    _searchCtrl.addListener(() {
      final q = _searchCtrl.text.trim();
      if (q != _query) setState(() => _query = q);
    });
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<List<Scoop>> _load({bool force = false}) async {
    final scoops = await RotterService.instance.fetchScoops();
    _sweepReadThreads(scoops, force: force);
    return scoops;
  }

  // Throttle the (network-heavy) sweep and cap how many threads it checks.
  static DateTime? _lastSweep;
  static const _sweepInterval = Duration(seconds: 90);
  static const _sweepCap = 25;

  /// For read threads, re-check the live reply count: resolve a pending baseline
  /// or, if the count grew, flag "new comments" (drops back to unread).
  Future<void> _sweepReadThreads(List<Scoop> scoops, {bool force = false}) async {
    final now = DateTime.now();
    if (!force && _lastSweep != null && now.difference(_lastSweep!) < _sweepInterval) {
      return;
    }
    _lastSweep = now;

    final read =
        scoops.where((s) => ReadStore.instance.isRead(s.id)).take(_sweepCap).toList();
    const concurrency = 4;
    for (var i = 0; i < read.length; i += concurrency) {
      final batch = read.skip(i).take(concurrency);
      await Future.wait(batch.map((s) async {
        try {
          final count = await RotterService.instance.fetchReplyCount(s.id);
          if (ReadStore.instance.isPending(s.id)) {
            await ReadStore.instance.markRead(s.id, count);
          } else if (count > (ReadStore.instance.seenCount(s.id) ?? count)) {
            await ReadStore.instance.markNewComments(s.id);
          }
        } catch (_) {/* leave as-is on error */}
      }));
      if (!mounted) return;
    }
  }

  Future<void> _refresh() async {
    // Drop cached per-card meta so reply counts + last-comment order refresh, and
    // force the read-sweep past its throttle (a manual pull should always update).
    ScoopMetaCache.instance.invalidate();
    final f = _load(force: true);
    setState(() {
      _future = f;
    });
    await f;
  }

  Future<void> _toggleRead(Scoop s) async {
    HapticFeedback.mediumImpact();
    if (ReadStore.instance.isRead(s.id)) {
      await ReadStore.instance.markUnread(s.id);
    } else {
      await ReadStore.instance.markRead(s.id, ReadStore.pending);
      try {
        final count = await RotterService.instance.fetchReplyCount(s.id);
        if (ReadStore.instance.isPending(s.id)) {
          await ReadStore.instance.markRead(s.id, count);
        }
      } catch (_) {/* stays pending */}
    }
  }

  void _stopSearch() {
    _searchCtrl.clear();
    setState(() {
      _searching = false;
      _query = '';
    });
  }

  Future<void> _markAllRead() async {
    if (_loaded.isEmpty) return;
    final l = L10n.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: Text(l.markAllReadConfirm),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l.cancel)),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l.markAllRead)),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    HapticFeedback.mediumImpact();
    await ReadStore.instance.markAllRead(_loaded.map((s) => s.id));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l.markedAllRead), duration: const Duration(seconds: 2)));
  }

  Future<void> _replyTo(Scoop s) async {
    HapticFeedback.mediumImpact();
    await Navigator.of(context).push(modernRoute(
      ComposeScreen(threadId: s.id, parentNum: 0),
    ));
  }

  _Bucket _bucketOf(DateTime? d) {
    if (d == null) return _Bucket.earlier;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final day = DateTime(d.year, d.month, d.day);
    final diff = today.difference(day).inDays;
    if (diff <= 0) return _Bucket.today;
    if (diff == 1) return _Bucket.yesterday;
    return _Bucket.earlier;
  }

  String _bucketLabel(_Bucket b, L10n l) => switch (b) {
        _Bucket.today => l.today,
        _Bucket.yesterday => l.yesterday,
        _Bucket.earlier => l.earlier,
      };

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    // A single plain ListView (one sliver) — a multi-sliver CustomScrollView
    // trips a Flutter semantics null-geometry crash. The top bar hides on
    // scroll-down via ScrollHidingScaffold (no slivers).
    return ScrollHidingScaffold(
      bar: _searching
          ? AppBar(
              primary: false,
              titleSpacing: 12,
              title: CupertinoSearchTextField(
                controller: _searchCtrl,
                autofocus: true,
                placeholder: l.searchScoops,
                style: TextStyle(color: cInk(context)),
                backgroundColor: cField(context),
                itemColor: cMuted(context),
              ),
              actions: [
                IconButton(
                  tooltip: l.cancel,
                  icon: const Icon(Icons.close_rounded),
                  onPressed: _stopSearch,
                ),
              ],
            )
          : AppBar(
              primary: false,
              title: Text(l.tabScoops),
              actions: [
                IconButton(
                  tooltip: l.markAllRead,
                  icon: const Icon(Icons.playlist_add_check_rounded),
                  onPressed: _markAllRead,
                ),
                IconButton(
                  tooltip: l.searchScoops,
                  icon: const Icon(Icons.search_rounded),
                  onPressed: () => setState(() => _searching = true),
                ),
              ],
            ),
      // Content is always Hebrew → force RTL even if the UI language is English.
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: RefreshIndicator(
          onRefresh: _refresh,
          child: FutureBuilder<List<Scoop>>(
            future: _future,
            builder: (context, snap) {
              if (snap.connectionState == ConnectionState.waiting) {
                return _skeletonList();
              }
              if (snap.hasError) {
                return _messageState(
                    icon: Icons.cloud_off_rounded,
                    text: l.loadingError,
                    action: FilledButton.tonal(onPressed: _refresh, child: Text(l.retry)));
              }
              final all = snap.data ?? const <Scoop>[];
              _loaded = all;
              final q = _query.toLowerCase();
              final scoops = q.isEmpty
                  ? all
                  : all.where((s) => s.title.toLowerCase().contains(q)).toList();
              if (scoops.isEmpty) {
                return _messageState(
                    icon: q.isEmpty ? Icons.inbox_rounded : Icons.search_off_rounded,
                    text: q.isEmpty ? l.emptyScoops : l.noResults);
              }
              return _list(scoops, l);
            },
          ),
        ),
      ),
    );
  }

  /// Flat list: section-header strings interleaved with scoop cards, ordered by
  /// the chosen sort mode.
  Widget _list(List<Scoop> scoops, L10n l) {
    return ValueListenableBuilder<SortMode>(
      valueListenable: SettingsController.instance.sortMode,
      builder: (context, sortMode, _) {
        // Post-time order never changes as meta loads → no live rebuild needed.
        // Last-comment order does, but throttle it so a burst of meta arrivals
        // doesn't re-sort + rebuild the whole list every frame (the choppiness).
        if (sortMode == SortMode.postTime) {
          return _sortedList(scoops, l, sortMode);
        }
        return _ThrottledBuilder(
          listenable: ScoopMetaCache.instance,
          interval: const Duration(milliseconds: 600),
          builder: (context) => _sortedList(scoops, l, sortMode),
        );
      },
    );
  }

  /// The effective time a scoop is sorted/grouped by: post time, or its last
  /// comment (falling back to post time until that meta has loaded).
  DateTime _effTime(Scoop s, SortMode mode) {
    final published = s.published ?? DateTime.fromMillisecondsSinceEpoch(0);
    if (mode == SortMode.postTime) return published;
    final last = ScoopMetaCache.instance.of(s.id)?.lastComment;
    return last != null && last.isAfter(published) ? last : published;
  }

  Widget _sortedList(List<Scoop> scoops, L10n l, SortMode sortMode) {
    // "Last comment" relies on each card's lazily-fetched meta (cards call
    // ensure() as they build); visible rows settle into last-comment order as
    // their meta arrives, while not-yet-loaded rows hold their post-time slot.
    final sorted = [...scoops]
      ..sort((a, b) => _effTime(b, sortMode).compareTo(_effTime(a, sortMode)));

    // Render ALL items — ListView.builder is already lazy (only visible rows are
    // built, so only they fetch meta), so there's no need to cap the count.
    final entries = <Object>[];
    _Bucket? current;
    for (final s in sorted) {
      final b = _bucketOf(_effTime(s, sortMode));
      if (b != current) {
        current = b;
        entries.add(_bucketLabel(b, l));
      }
      entries.add(s);
    }
    // Clearance so the last card clears the translucent bottom tab bar (extendBody).
    final bottomInset = MediaQuery.paddingOf(context).bottom + 12;
    return ListView.builder(
        // Top inset clears the overlaid (hideable) app bar.
        padding: EdgeInsets.only(top: ScrollHidingScaffold.barHeight, bottom: bottomInset),
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: entries.length,
        itemBuilder: (context, i) {
          final e = entries[i];
          if (e is String) {
            return Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 6),
              child: Text(e,
                  style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.6,
                      color: cMuted(context))),
            );
          }
          final s = e as Scoop;
          return Padding(
            padding: const EdgeInsets.fromLTRB(14, 0, 14, 10),
            child: _SwipeRow(
              scoop: s,
              onToggleRead: () => _toggleRead(s),
              onReply: () => _replyTo(s),
              onOpen: () {
                HapticFeedback.selectionClick();
                Navigator.of(context).push(modernRoute(ThreadScreen(scoop: s)));
              },
            ),
          );
        },
      );
  }

  Widget _skeletonList() {
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(14, 14 + ScrollHidingScaffold.barHeight, 14, 8),
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: 7,
      separatorBuilder: (_, __) => const SizedBox(height: 10),
      itemBuilder: (context, i) => _SkeletonCard(),
    );
  }

  Widget _messageState({required IconData icon, required String text, Widget? action}) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: [
        SizedBox(height: MediaQuery.of(context).size.height * 0.3),
        Icon(icon, size: 46, color: cMuted(context)),
        const SizedBox(height: 12),
        Center(child: Text(text, style: TextStyle(color: cMuted(context)))),
        if (action != null) ...[const SizedBox(height: 14), Center(child: action)],
      ],
    );
  }
}

// ---------------------------------------------------------------------------

class _SwipeRow extends StatelessWidget {
  final Scoop scoop;
  final VoidCallback onToggleRead;
  final VoidCallback onReply;
  final VoidCallback onOpen;

  const _SwipeRow({
    required this.scoop,
    required this.onToggleRead,
    required this.onReply,
    required this.onOpen,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    return AnimatedBuilder(
      animation: Listenable.merge([ReadStore.instance, MyRepliesStore.instance]),
      builder: (context, _) {
        final read = ReadStore.instance.isRead(scoop.id);
        return ValueListenableBuilder<bool>(
          valueListenable: AuthService.instance.loggedIn,
          builder: (context, loggedIn, __) => Dismissible(
          key: ValueKey(scoop.id),
          // Swipe-to-reply (startToEnd) is disabled until signed in; mark-read stays.
          direction:
              loggedIn ? DismissDirection.horizontal : DismissDirection.endToStart,
          confirmDismiss: (dir) async {
            if (dir == DismissDirection.startToEnd) {
              onReply();
            } else {
              onToggleRead();
            }
            return false;
          },
          background: _action(
            context,
            align: AlignmentDirectional.centerStart,
            color: accent,
            icon: Icons.reply_rounded,
            label: l.reply,
          ),
          secondaryBackground: _action(
            context,
            align: AlignmentDirectional.centerEnd,
            color: read ? cMuted(context) : Colors.green.shade600,
            icon: read ? Icons.mark_email_unread_rounded : Icons.check_circle_rounded,
            label: read ? l.markUnread : l.markRead,
          ),
          child: _ScoopCard(scoop: scoop, read: read, onOpen: onOpen),
        ),
        );
      },
    );
  }

  Widget _action(BuildContext context,
      {required AlignmentGeometry align,
      required Color color,
      required IconData icon,
      required String label}) {
    return Container(
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(20)),
      padding: const EdgeInsets.symmetric(horizontal: 22),
      alignment: align,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: Colors.white),
          const SizedBox(width: 8),
          Text(label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

class _ScoopCard extends StatelessWidget {
  final Scoop scoop;
  final bool read;
  final VoidCallback onOpen;
  const _ScoopCard({required this.scoop, required this.read, required this.onOpen});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    final isNew = ReadStore.instance.isNew(scoop.id);
    final mine = MyRepliesStore.instance.replied(scoop.id);
    ScoopMetaCache.instance.ensure(scoop.id);
    final when = scoop.published == null ? null : relTime(scoop.published!, context);

    return AnimatedOpacity(
      duration: const Duration(milliseconds: 220),
      opacity: read ? 0.55 : 1,
      child: Container(
        decoration: BoxDecoration(
          color: cSurface(context),
          borderRadius: BorderRadius.circular(20),
          // Green side when the user has replied in this thread.
          border: mine
              ? BorderDirectional(start: BorderSide(color: Colors.green.shade600, width: 4))
              : null,
          boxShadow: [
            BoxShadow(
                color: Colors.black.withValues(alpha: read ? 0.02 : 0.05),
                blurRadius: 16,
                offset: const Offset(0, 6)),
          ],
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onOpen,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 9,
                  height: 9,
                  margin: const EdgeInsetsDirectional.only(end: 11, top: 7),
                  decoration: BoxDecoration(
                    color: read ? cMuted(context).withValues(alpha: 0.35) : accent,
                    shape: BoxShape.circle,
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        scoop.title,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              height: 1.3,
                              fontSize: 16.5,
                              fontWeight: read ? FontWeight.w600 : FontWeight.w800,
                              letterSpacing: -0.2,
                            ),
                      ),
                      const SizedBox(height: 9),
                      _MetaLine(scoop: scoop, when: when, isNew: isNew),
                    ],
                  ),
                ),
                Icon(Icons.chevron_left_rounded, color: cMuted(context)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Author · replies · time, plus a "new" pill — author/replies arrive lazily.
class _MetaLine extends StatelessWidget {
  final Scoop scoop;
  final String? when;
  final bool isNew;
  const _MetaLine({required this.scoop, required this.when, required this.isNew});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final muted = cMuted(context);
    final style = TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: muted);

    return AnimatedBuilder(
      animation: ScoopMetaCache.instance,
      builder: (context, _) {
        final meta = ScoopMetaCache.instance.of(scoop.id);
        // A 404 on an older scoop = removed (deleted by a moderator); it lingers
        // in the RSS feed until that regenerates. Flag it so it's not mistaken
        // for a live thread.
        final removed = (meta?.unavailable ?? false) &&
            scoop.published != null &&
            DateTime.now().difference(scoop.published!) > const Duration(minutes: 15);
        final bits = <Widget>[];
        if (removed) {
          bits.add(Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
                color: Colors.red.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(999)),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Icon(Icons.delete_outline_rounded, size: 12, color: Colors.red.shade400),
              const SizedBox(width: 3),
              Text(l.removedBadge,
                  style: TextStyle(
                      fontSize: 11, fontWeight: FontWeight.w800, color: Colors.red.shade400)),
            ]),
          ));
        }
        if (isNew && !removed) {
          bits.add(Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
                color: accent.withValues(alpha: 0.14), borderRadius: BorderRadius.circular(999)),
            child: Text(l.newBadge,
                style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: accent)),
          ));
        }
        if (meta?.author != null) {
          bits.add(_iconText(Icons.person_rounded, meta!.author!, style, muted));
        }
        if (meta?.replies != null) {
          bits.add(_iconText(
              Icons.mode_comment_outlined, '${meta!.replies}', style, muted));
        }
        if (when != null) bits.add(Text(when!, style: style));

        return Wrap(
          spacing: 12,
          runSpacing: 4,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: bits,
        );
      },
    );
  }

  Widget _iconText(IconData icon, String text, TextStyle style, Color color) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 3),
          Text(text, style: style),
        ],
      );
}

class _SkeletonCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    Widget bar(double w, double h) => Container(
          width: w,
          height: h,
          decoration: BoxDecoration(
            color: cField(context),
            borderRadius: BorderRadius.circular(6),
          ),
        );
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 18),
      decoration: BoxDecoration(
        color: cSurface(context),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          bar(double.infinity, 14),
          const SizedBox(height: 9),
          bar(220, 14),
          const SizedBox(height: 14),
          bar(140, 11),
        ],
      ),
    ).animate(onPlay: (c) => c.repeat()).shimmer(
        duration: 1100.ms, color: cMuted(context).withValues(alpha: 0.12));
  }
}

/// Rebuilds its [builder] when [listenable] fires, but at most once per
/// [interval] — so a burst of metadata arrivals re-sorts the list a couple of
/// times a second instead of every frame.
class _ThrottledBuilder extends StatefulWidget {
  final Listenable listenable;
  final Duration interval;
  final WidgetBuilder builder;
  const _ThrottledBuilder(
      {required this.listenable, required this.interval, required this.builder});

  @override
  State<_ThrottledBuilder> createState() => _ThrottledBuilderState();
}

class _ThrottledBuilderState extends State<_ThrottledBuilder> {
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    widget.listenable.addListener(_onChange);
  }

  @override
  void dispose() {
    _timer?.cancel();
    widget.listenable.removeListener(_onChange);
    super.dispose();
  }

  void _onChange() {
    if (_timer?.isActive ?? false) return; // already a rebuild scheduled
    _timer = Timer(widget.interval, () {
      if (mounted) setState(() {});
    });
  }

  @override
  Widget build(BuildContext context) => widget.builder(context);
}
