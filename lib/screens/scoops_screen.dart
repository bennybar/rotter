import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../l10n/app_localizations.dart';
import '../models/scoop.dart';
import '../nav.dart';
import '../services/disk_cache.dart';
import '../services/my_replies_store.dart';
import '../services/read_store.dart';
import '../services/rotter_service.dart';
import '../services/saved_store.dart';
import '../services/scoop_meta.dart';
import '../services/settings_controller.dart';
import '../theme.dart';
import '../util/rel_time.dart';
import '../widgets/member_chips.dart';
import '../widgets/scroll_hiding_scaffold.dart';
import 'compose_screen.dart';
import 'thread_screen.dart';

enum _Bucket { today, yesterday, earlier }

/// Which scoops the list shows.
enum ScoopFilter { all, unread, newComments, mine, saved, following }

class ScoopsScreen extends StatefulWidget {
  const ScoopsScreen({super.key});

  @override
  State<ScoopsScreen> createState() => _ScoopsScreenState();
}

class _ScoopsScreenState extends State<ScoopsScreen> with WidgetsBindingObserver {
  static const _listFile = 'scoops.json';

  /// Roughly two screens of rows — what post-time order prefetches.
  static const _firstScreenful = 20;

  final _meta = ScoopMetaCache.instance;
  final _searchCtrl = TextEditingController();
  String _query = '';
  bool _searching = false; // toggles the in-app-bar search field
  ScoopFilter _filter = ScoopFilter.all;

  /// The list on screen. Seeded from disk so a launch paints instantly and the
  /// feed request becomes a refresh of something already there.
  List<Scoop> _loaded = const [];

  /// A feed fetched on return from the background, held back behind the
  /// "new scoops" pill instead of reshuffling the list under the reader.
  List<Scoop>? _pending;

  bool _didFetch = false; // a network load completed this session
  bool _failed = false; // first load failed (nothing to show)
  bool _refreshFailed = false; // a refresh failed while a list was showing
  Timer? _refreshFailedTimer;
  final _feedLoading = ValueNotifier<bool>(false);
  Future<void>? _inFlight;
  DateTime? _backgroundedAt;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    SettingsController.instance.sortMode.addListener(_onSortChanged);
    _searchCtrl.addListener(() {
      final q = _searchCtrl.text.trim();
      if (q != _query) setState(() => _query = q);
    });
    _start();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    SettingsController.instance.sortMode.removeListener(_onSortChanged);
    _refreshFailedTimer?.cancel();
    _feedLoading.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    final cached = await DiskCache.read(_listFile);
    if (cached is List && cached.isNotEmpty && mounted && !_didFetch) {
      setState(() => _loaded = [
            for (final j in cached) Scoop.fromJson(Map<String, dynamic>.from(j as Map)),
          ]);
    }
    await _load();
    _meta.prefetch(SavedStore.followed.scoops.map((s) => s.id));
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) _backgroundedAt = DateTime.now();
    if (state == AppLifecycleState.resumed && _backgroundedAt != null) {
      final away = DateTime.now().difference(_backgroundedAt!);
      _backgroundedAt = null;
      if (away < const Duration(minutes: 2)) return;
      // Check quietly: keep the rows where they are and offer the new feed.
      _meta.setOrderingPaused(true);
      _meta.prefetch(SavedStore.followed.scoops.map((s) => s.id));
      _inFlight ??= _performLoad(stage: true).whenComplete(() => _inFlight = null);
    }
  }

  void _onSortChanged() {
    _meta.setOrderingPaused(false);
    // Last-comment order needs a time for every thread, not just fetched ones.
    if (SettingsController.instance.sortMode.value == SortMode.lastComment) {
      _meta.prefetch(_loaded.map((s) => s.id));
    }
  }

  /// Loads the feed, joining a load already in progress rather than racing it
  /// (an older response landing last would overwrite a newer one).
  Future<void> _load() async {
    final existing = _inFlight;
    if (existing != null) {
      await existing;
      _applyPending();
      // A pull that arrived mid-load has since marked metadata stale.
      _prefetchMeta(_loaded);
      return;
    }
    final f = _performLoad();
    _inFlight = f;
    await f;
    _inFlight = null;
  }

  Future<void> _performLoad({bool stage = false}) async {
    _feedLoading.value = true;
    try {
      final scoops = await RotterService.instance.fetchScoops();
      if (!mounted) return;
      _didFetch = true;
      if (stage && _loaded.isNotEmpty) {
        setState(() {
          _refreshFailed = false;
          _pending = listEquals(scoops, _loaded) ? null : scoops;
        });
        return;
      }
      setState(() {
        _pending = null;
        _loaded = scoops;
        _failed = false;
        _refreshFailed = false;
      });
      _persist(scoops);
      _prefetchMeta(scoops);
    } catch (_) {
      if (!mounted) return;
      // A first load that fails shows the error state; a failed REFRESH keeps
      // the list being read, but says so.
      setState(() {
        _failed = _loaded.isEmpty;
        _refreshFailed = _loaded.isNotEmpty;
      });
      _refreshFailedTimer?.cancel();
      _refreshFailedTimer = Timer(const Duration(seconds: 5), () {
        if (mounted) setState(() => _refreshFailed = false);
      });
    } finally {
      _feedLoading.value = false;
    }
  }

  void _applyPending() {
    final pending = _pending;
    if (pending == null || !mounted) return;
    setState(() {
      _loaded = pending;
      _pending = null;
    });
    _persist(pending);
    _meta.revalidate();
    _prefetchMeta(pending);
  }

  void _persist(List<Scoop> scoops) =>
      DiskCache.write(_listFile, [for (final s in scoops) s.toJson()]);

  /// Last-comment order needs a time for EVERY thread (else an older thread
  /// that just got a reply can never rise). Post-time order only needs a
  /// screenful. Either way read threads are included: their live reply count
  /// is how new comments are noticed.
  void _prefetchMeta(List<Scoop> scoops) {
    if (SettingsController.instance.sortMode.value == SortMode.lastComment) {
      _meta.prefetch(scoops.map((s) => s.id));
      return;
    }
    final first = scoops.take(_firstScreenful).map((s) => s.id).toSet();
    _meta.prefetch([
      ...first,
      for (final s in scoops)
        if (ReadStore.instance.isRead(s.id) && !first.contains(s.id)) s.id,
    ]);
  }

  Future<void> _refresh() async {
    // Re-fetch card meta even inside its freshness window — a manual pull
    // should always update. Old values stay on screen until replaced.
    _meta.setOrderingPaused(false);
    _meta.revalidate();
    await _load();
    _meta.prefetch(SavedStore.followed.scoops.map((s) => s.id));
  }

  Future<void> _toggleRead(Scoop s) async {
    HapticFeedback.mediumImpact();
    if (ReadStore.instance.isRead(s.id)) {
      await ReadStore.instance.markUnread(s.id);
      return;
    }
    // Use the count the card is already showing rather than refetching it.
    final known = _meta.of(s.id)?.replies;
    if (known != null) {
      await ReadStore.instance.markRead(s.id, known);
      return;
    }
    // Not fetched yet: mark read now and let the metadata arrival fill in the
    // baseline (reconcile resolves the pending state).
    await ReadStore.instance.markRead(s.id, ReadStore.pending);
    _meta.ensure(s.id);
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
    await openComposer(context, ComposeScreen(threadId: s.id, parentNum: 0));
  }

  Future<void> _showActions(Scoop s) async {
    HapticFeedback.mediumImpact();
    final l = L10n.of(context)!;
    final saved = SavedStore.saved.contains(s.id);
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(
            leading: Icon(saved ? Icons.bookmark_remove_rounded : Icons.bookmark_add_outlined),
            title: Text(saved ? l.unsaveScoop : l.saveScoop),
            onTap: () {
              Navigator.pop(ctx);
              SavedStore.saved.toggle(s);
            },
          ),
          ListTile(
            leading: const Icon(Icons.ios_share_rounded),
            title: Text(l.share),
            onTap: () {
              Navigator.pop(ctx);
              SharePlus.instance.share(ShareParams(uri: Uri.parse(s.url)));
            },
          ),
          ListTile(
            leading: const Icon(Icons.open_in_browser_rounded),
            title: Text(l.openOnRotter),
            onTap: () {
              Navigator.pop(ctx);
              launchUrl(Uri.parse(s.url), mode: LaunchMode.externalApplication);
            },
          ),
        ]),
      ),
    );
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

  String _filterLabel(ScoopFilter f, L10n l) => switch (f) {
        ScoopFilter.all => l.filterAll,
        ScoopFilter.unread => l.filterUnread,
        ScoopFilter.newComments => l.filterNewComments,
        ScoopFilter.mine => l.filterMine,
        ScoopFilter.saved => l.filterSaved,
        ScoopFilter.following => l.filterFollowing,
      };

  IconData _filterIcon(ScoopFilter f) => switch (f) {
        ScoopFilter.all => Icons.inbox_rounded,
        ScoopFilter.unread => Icons.circle,
        ScoopFilter.newComments => Icons.auto_awesome_rounded,
        ScoopFilter.mine => Icons.forum_outlined,
        ScoopFilter.saved => Icons.bookmark_outline_rounded,
        ScoopFilter.following => Icons.notifications_none_rounded,
      };

  String _emptyText(ScoopFilter f, L10n l) => switch (f) {
        ScoopFilter.all => l.emptyScoops,
        ScoopFilter.unread => l.emptyUnread,
        ScoopFilter.newComments => l.emptyNewComments,
        ScoopFilter.mine => l.noMyReplies,
        ScoopFilter.saved => l.emptySaved,
        ScoopFilter.following => l.emptyFollowing,
      };

  bool _matches(ScoopFilter f, Scoop s) => switch (f) {
        ScoopFilter.all => true,
        ScoopFilter.unread => !ReadStore.instance.isRead(s.id),
        ScoopFilter.newComments => ReadStore.instance.isNew(s.id),
        ScoopFilter.mine => MyRepliesStore.instance.replied(s.id),
        ScoopFilter.saved => SavedStore.saved.contains(s.id),
        ScoopFilter.following => SavedStore.followed.contains(s.id),
      };

  /// Saved/followed threads may have left the rolling feed, so those filters
  /// read from their own stores rather than the feed.
  List<Scoop> _source(ScoopFilter f) => switch (f) {
        ScoopFilter.saved => SavedStore.saved.scoops,
        ScoopFilter.following => SavedStore.followed.scoops,
        _ => _loaded,
      };

  int _count(ScoopFilter f) => _source(f).where((s) => _matches(f, s)).length;

  /// The effective time a scoop is sorted/grouped by: post time, or its last
  /// comment (falling back to post time until that meta has loaded). Reads the
  /// throttled ordering snapshot, not the live cache.
  DateTime _effTime(Scoop s, SortMode mode) {
    final published = s.published ?? DateTime.fromMillisecondsSinceEpoch(0);
    if (mode == SortMode.postTime) return published;
    final last = _meta.orderingTimes.value[s.id];
    return last != null && last.isAfter(published) ? last : published;
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final chrome = Directionality.of(context); // interface direction, for chrome
    // A single plain ListView (one sliver) — a multi-sliver CustomScrollView
    // trips a Flutter semantics null-geometry crash. The top bar hides on
    // scroll-down via ScrollHidingScaffold (no slivers).
    return ScrollHidingScaffold(
      bar: PreferredSize(
        preferredSize: const Size.fromHeight(ScrollHidingScaffold.barHeight),
        child: Stack(children: [
          Positioned.fill(child: _searching ? _searchBar(l) : _titleBar(l)),
          // Work still happening behind a list that already looks finished
          // (it paints from disk): the feed request, then the card backfill.
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: _RefreshBar(meta: _meta, feedLoading: _feedLoading),
          ),
        ]),
      ),
      // Content is always Hebrew → force RTL even if the UI language is English.
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: Stack(children: [
          Positioned.fill(
            child: RefreshIndicator(onRefresh: _refresh, child: _content(l)),
          ),
          if (_pending != null)
            Positioned(
              top: ScrollHidingScaffold.barHeight + 10,
              left: 0,
              right: 0,
              child: Directionality(
                textDirection: chrome,
                child: Center(child: _updatePill(l)),
              ),
            ),
          Positioned(
            bottom: MediaQuery.paddingOf(context).bottom + 76,
            left: 0,
            right: 0,
            child: Directionality(
              textDirection: chrome,
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 250),
                child: _refreshFailed ? Center(child: _toast(l.refreshFailed)) : null,
              ),
            ),
          ),
        ]),
      ),
    );
  }

  PreferredSizeWidget _searchBar(L10n l) => AppBar(
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
      );

  PreferredSizeWidget _titleBar(L10n l) {
    final active = _filter != ScoopFilter.all;
    return AppBar(
      primary: false,
      title: Text(active ? _filterLabel(_filter, l) : l.tabScoops),
      actions: [
        IconButton(
          tooltip: l.searchScoops,
          icon: const Icon(Icons.search_rounded),
          onPressed: () => setState(() => _searching = true),
        ),
        // Filter, sort and mark-all-read in one labelled menu: as bare glyphs
        // these were undiscoverable, and sort is something you change while
        // reading the list, not while configuring the app.
        PopupMenuButton<Object>(
          tooltip: l.filterTitle,
          icon: Icon(active ? Icons.filter_alt_rounded : Icons.filter_list_rounded,
              color: active ? Theme.of(context).colorScheme.primary : null),
          onSelected: (v) {
            HapticFeedback.selectionClick();
            if (v is ScoopFilter) setState(() => _filter = v);
            if (v is SortMode) SettingsController.instance.setSortMode(v);
            if (v == 'markAll') _markAllRead();
          },
          itemBuilder: (context) {
            final sort = SettingsController.instance.sortMode.value;
            return [
              for (final f in ScoopFilter.values)
                CheckedPopupMenuItem<Object>(
                  value: f,
                  checked: _filter == f,
                  child: Text('${_filterLabel(f, l)} (${_count(f)})'),
                ),
              const PopupMenuDivider(),
              PopupMenuItem<Object>(
                enabled: false,
                height: 32,
                child: Text(l.sortBy, style: TextStyle(fontSize: 12.5, color: cMuted(context))),
              ),
              CheckedPopupMenuItem<Object>(
                value: SortMode.lastComment,
                checked: sort == SortMode.lastComment,
                child: Text(l.sortLastComment),
              ),
              CheckedPopupMenuItem<Object>(
                value: SortMode.postTime,
                checked: sort == SortMode.postTime,
                child: Text(l.sortPostTime),
              ),
              const PopupMenuDivider(),
              PopupMenuItem<Object>(
                value: 'markAll',
                enabled: _loaded.isNotEmpty,
                child: Row(children: [
                  const Icon(Icons.playlist_add_check_rounded, size: 20),
                  const SizedBox(width: 10),
                  Text(l.markAllRead),
                ]),
              ),
            ];
          },
        ),
      ],
    );
  }

  Widget _updatePill(L10n l) {
    final ids = _loaded.map((s) => s.id).toSet();
    final hasNew = _pending?.any((s) => !ids.contains(s.id)) ?? false;
    return Material(
      color: Theme.of(context).colorScheme.primary,
      shape: const StadiumBorder(),
      elevation: 3,
      child: InkWell(
        customBorder: const StadiumBorder(),
        onTap: () {
          HapticFeedback.selectionClick();
          _meta.setOrderingPaused(false);
          _applyPending();
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
          child: Row(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.arrow_upward_rounded, size: 17, color: Colors.white),
            const SizedBox(width: 6),
            Text(hasNew ? l.feedNewScoops : l.feedUpdated,
                style: const TextStyle(
                    color: Colors.white, fontWeight: FontWeight.w700, fontSize: 13.5)),
          ]),
        ),
      ),
    ).animate().fadeIn(duration: 200.ms).slideY(begin: -0.4, end: 0);
  }

  Widget _toast(String text) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: cSurface(context),
          borderRadius: BorderRadius.circular(999),
          boxShadow: [
            BoxShadow(color: Colors.black.withValues(alpha: 0.12), blurRadius: 14),
          ],
        ),
        child: Text(text, style: TextStyle(fontSize: 13, color: cInk(context))),
      );

  Widget _content(L10n l) {
    if (_loaded.isEmpty && !_didFetch && !_failed) return _skeletonList();
    if (_failed) {
      return _messageState(
          icon: Icons.cloud_off_rounded,
          text: l.loadingError,
          action: FilledButton.tonal(onPressed: _refresh, child: Text(l.retry)));
    }
    return AnimatedBuilder(
      animation: Listenable.merge([
        ReadStore.instance,
        MyRepliesStore.instance,
        SavedStore.saved,
        SavedStore.followed,
        _meta.orderingTimes,
        SettingsController.instance.sortMode,
      ]),
      builder: (context, _) {
        final q = _query.toLowerCase();
        final scoops = _source(_filter)
            .where((s) => q.isEmpty || s.title.toLowerCase().contains(q))
            .where((s) => _matches(_filter, s))
            .toList();
        if (scoops.isEmpty) {
          if (q.isNotEmpty) {
            return _messageState(icon: Icons.search_off_rounded, text: l.noResults);
          }
          if (_filter != ScoopFilter.all) {
            return _messageState(
                icon: _filterIcon(_filter),
                text: _emptyText(_filter, l),
                action: FilledButton.tonal(
                    onPressed: () => setState(() => _filter = ScoopFilter.all),
                    child: Text(l.filterShowAll)));
          }
          return _messageState(
              icon: Icons.inbox_rounded,
              text: l.emptyScoops,
              action: FilledButton.tonal(onPressed: _refresh, child: Text(l.retry)));
        }
        return _sortedList(scoops, l, SettingsController.instance.sortMode.value);
      },
    );
  }

  /// Flat list: section-header strings interleaved with scoop cards, ordered by
  /// the chosen sort mode.
  Widget _sortedList(List<Scoop> scoops, L10n l, SortMode sortMode) {
    final sorted = [...scoops]..sort((a, b) {
        final c = _effTime(b, sortMode).compareTo(_effTime(a, sortMode));
        return c != 0 ? c : b.id.compareTo(a.id);
      });

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
    return NotificationListener<ScrollStartNotification>(
      // Keep rows in place once the reader starts scrolling, so a metadata
      // arrival can't move the row under their thumb.
      onNotification: (n) {
        if (n.dragDetails != null) _meta.setOrderingPaused(true);
        return false;
      },
      child: ListView.builder(
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
              onLongPress: () => _showActions(s),
              onOpen: () {
                HapticFeedback.selectionClick();
                Navigator.of(context).push(modernRoute(ThreadScreen(scoop: s)));
              },
            ),
          );
        },
      ),
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
  final VoidCallback onLongPress;

  const _SwipeRow({
    required this.scoop,
    required this.onToggleRead,
    required this.onReply,
    required this.onOpen,
    required this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final read = ReadStore.instance.isRead(scoop.id);
    return Dismissible(
      key: ValueKey(scoop.id),
      // Swipe-to-reply works signed out too: it signs in first, then opens the
      // composer that was asked for.
      direction: DismissDirection.horizontal,
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
      child: _ScoopCard(scoop: scoop, read: read, onOpen: onOpen, onLongPress: onLongPress),
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
  final VoidCallback onLongPress;
  const _ScoopCard(
      {required this.scoop, required this.read, required this.onOpen, required this.onLongPress});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    final mine = MyRepliesStore.instance.replied(scoop.id);
    ScoopMetaCache.instance.ensure(scoop.id);

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
          onLongPress: onLongPress,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
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
                  child: AnimatedBuilder(
                    animation: Listenable.merge([ScoopMetaCache.instance, SavedStore.saved]),
                    builder: (context, _) => Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _PosterLine(scoop: scoop),
                        const SizedBox(height: 8),
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
                        _MetaLine(scoop: scoop),
                      ],
                    ),
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

/// A grey stand-in the size of the value it's waiting for, so the card holds
/// its height while metadata loads instead of growing a line mid-scroll.
Widget _placeholder(BuildContext context, double w, double h) => Container(
      width: w,
      height: h,
      decoration: BoxDecoration(color: cField(context), borderRadius: BorderRadius.circular(5)),
    );

/// Poster (avatar + name + points) above the headline, posting time opposite.
/// Deliberately not tappable — the card has one action, opening the thread.
class _PosterLine extends StatelessWidget {
  final Scoop scoop;
  const _PosterLine({required this.scoop});

  @override
  Widget build(BuildContext context) {
    final meta = ScoopMetaCache.instance.of(scoop.id);
    final pending = ScoopMetaCache.instance.isPending(scoop.id);
    final author = meta?.author;
    final muted = cMuted(context);
    return Row(children: [
      if (author != null) ...[
        avatarBubble(author, 21),
        const SizedBox(width: 7),
        Flexible(
          child: Text(author,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.w700, color: cInk(context))),
        ),
        if (meta?.authorPoints != null) ...[
          const SizedBox(width: 6),
          pointsChip(context, meta!.authorPoints!, null),
        ],
      ] else if (pending) ...[
        Container(
            width: 21,
            height: 21,
            decoration: BoxDecoration(color: cField(context), shape: BoxShape.circle)),
        const SizedBox(width: 7),
        _placeholder(context, 84, 12),
      ],
      const Spacer(),
      if (scoop.published != null)
        Text(relTime(scoop.published!, context),
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: muted)),
    ]);
  }
}

/// Saved · removed/new · replies · last activity — author/replies arrive lazily.
class _MetaLine extends StatelessWidget {
  final Scoop scoop;
  const _MetaLine({required this.scoop});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final muted = cMuted(context);
    final style = TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: muted);
    final cache = ScoopMetaCache.instance;
    final meta = cache.of(scoop.id);
    final pending = cache.isPending(scoop.id);
    final isNew = ReadStore.instance.isNew(scoop.id);

    // A 404 on an older scoop = removed (deleted by a moderator); it lingers in
    // the RSS feed until that regenerates. A FRESH 404 is just a page rotter
    // hasn't generated yet.
    final removed = cache.isUnavailable(scoop.id) &&
        scoop.published != null &&
        DateTime.now().difference(scoop.published!) > const Duration(minutes: 15);
    final bits = <Widget>[];
    if (SavedStore.saved.contains(scoop.id)) {
      bits.add(Icon(Icons.bookmark_rounded, size: 15, color: accent));
    }
    if (removed) {
      bits.add(_pill(l.removedBadge, Colors.red.shade400, icon: Icons.delete_outline_rounded));
    } else if (isNew) {
      bits.add(_pill(l.newBadge, accent));
    }
    if (meta?.replies != null) {
      bits.add(_iconText(Icons.mode_comment_outlined, '${meta!.replies}', style, muted));
    } else if (pending) {
      bits.add(_placeholder(context, 30, 11));
    }
    // When the thread was last active — what "sort by last comment" orders on;
    // a clock, so it doesn't read as another count.
    if (meta?.lastComment != null) {
      bits.add(_iconText(Icons.schedule_rounded, relTime(meta!.lastComment!, context), style, muted));
    } else if (pending) {
      bits.add(_placeholder(context, 56, 11));
    }

    return Wrap(
      spacing: 12,
      runSpacing: 4,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: bits,
    );
  }

  Widget _pill(String text, Color c, {IconData? icon}) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration:
            BoxDecoration(color: c.withValues(alpha: 0.13), borderRadius: BorderRadius.circular(999)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          if (icon != null) ...[Icon(icon, size: 12, color: c), const SizedBox(width: 3)],
          Text(text, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: c)),
        ]),
      );

  Widget _iconText(IconData icon, String text, TextStyle style, Color color) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 3),
          Text(text, style: style),
        ],
      );
}

/// A hairline under the app bar while the feed loads (indeterminate) and while
/// card metadata backfills (real progress). Holds its height when idle so the
/// list never nudges down and back on each refresh.
class _RefreshBar extends StatelessWidget {
  final ScoopMetaCache meta;
  final ValueListenable<bool> feedLoading;
  const _RefreshBar({required this.meta, required this.feedLoading});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    return SizedBox(
      height: 2.5,
      child: AnimatedBuilder(
        animation: Listenable.merge([meta.progress, feedLoading]),
        builder: (context, _) {
          final p = meta.progress.value;
          if (p == null && !feedLoading.value) return const SizedBox.shrink();
          final track = cField(context);
          if (p == null) {
            return LinearProgressIndicator(minHeight: 2.5, color: accent, backgroundColor: track);
          }
          // Eased, so an uneven jump (a 3KB and a 500KB thread count the same)
          // doesn't read as a glitch.
          return TweenAnimationBuilder<double>(
            tween: Tween(end: p.clamp(0, 1)),
            duration: const Duration(milliseconds: 350),
            curve: Curves.easeOut,
            builder: (context, v, _) => LinearProgressIndicator(
                value: v, minHeight: 2.5, color: accent, backgroundColor: track),
          );
        },
      ),
    );
  }
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
