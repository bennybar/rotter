import 'dart:async';

import 'package:flutter/foundation.dart';

import 'disk_cache.dart';
import 'read_store.dart';
import 'rotter_service.dart';

class ScoopMeta {
  final String? author;
  final int? authorPoints; // the poster's reputation, for the chip by their name
  final int? replies;
  final DateTime? lastComment;
  const ScoopMeta({this.author, this.authorPoints, this.replies, this.lastComment});

  Map<String, dynamic> toJson() => {
        if (author != null) 'author': author,
        if (authorPoints != null) 'authorPoints': authorPoints,
        if (replies != null) 'replies': replies,
        if (lastComment != null) 'lastComment': lastComment!.millisecondsSinceEpoch,
      };

  factory ScoopMeta.fromJson(Map<String, dynamic> j) => ScoopMeta(
        author: j['author'] as String?,
        authorPoints: (j['authorPoints'] as num?)?.toInt(),
        replies: (j['replies'] as num?)?.toInt(),
        lastComment: j['lastComment'] == null
            ? null
            : DateTime.fromMillisecondsSinceEpoch((j['lastComment'] as num).toInt()),
      );

  @override
  bool operator ==(Object other) =>
      other is ScoopMeta &&
      other.author == author &&
      other.authorPoints == authorPoints &&
      other.replies == replies &&
      other.lastComment == lastComment;

  @override
  int get hashCode => Object.hash(author, authorPoints, replies, lastComment);
}

/// Fetches and caches per-card metadata (poster, their points, reply count,
/// last-comment time) — none of which is in the RSS.
///
/// - **Bounded priority queue**: work waits its turn instead of being dropped
///   when the cap is hit; cards on screen jump to the front ([ensure]), the
///   rest of the feed is warmed behind them ([prefetch]) — needed for "sort by
///   last comment" to mean anything.
/// - **Persisted** to the cache dir, so a relaunch paints complete cards
///   instantly. Entries older than [_freshFor] are still *shown* and refreshed
///   underneath; [revalidate] (pull-to-refresh) marks everything stale without
///   blanking it, so only threads that actually changed move.
/// - **404 vs failure** are kept apart: a 404 is "the thread is gone" and not
///   retried; a network blip keeps whatever was shown and retries after a
///   cooldown, instead of leaving a permanent skeleton.
/// - Every network arrival feeds [ReadStore.reconcile], so read threads notice
///   new comments from this fetch rather than a second pass over the same pages.
class ScoopMetaCache extends ChangeNotifier {
  ScoopMetaCache({Future<CardMeta> Function(String id)? fetch, this.persists = true})
      : _fetch = fetch ?? RotterService.instance.fetchCardMeta;

  static final ScoopMetaCache instance = ScoopMetaCache();

  /// Measured on iOS: filling the whole feed takes 6.9s at 6, 2.6s at 20 — the
  /// work is latency-bound. 20 was checked against the live site: all requests
  /// 200, no Cloudflare pushback.
  static const _maxInFlight = 20;
  static const _fileName = 'scoop-meta.json';
  static const _freshFor = Duration(minutes: 10);
  static const _maxPersisted = 400;
  static const _retryCooldown = Duration(seconds: 30);

  final Future<CardMeta> Function(String id) _fetch;
  final bool persists;

  final Map<String, ScoopMeta> _loaded = {};
  final Set<String> _unavailable = {};
  final Map<String, DateTime> _fetchedAt = {};
  final Map<String, DateTime> _failedAt = {};
  final Set<String> _inFlight = {};
  final List<String> _queue = []; // nearest-to-the-viewport first
  final Set<String> _stale = {};
  int _batchTotal = 0;
  int _batchDone = 0;
  Timer? _saveTimer;

  /// Backfill progress 0..1 while card fetches are running, else null. Real
  /// progress — a known set of ids each resolving once — not an estimate.
  final ValueNotifier<double?> progress = ValueNotifier(null);

  /// Last-comment times the list sorts by, refreshed at most every 600ms: read
  /// live, every one of ~75 arrivals re-sorts and re-groups the whole list.
  final ValueNotifier<Map<String, DateTime>> orderingTimes = ValueNotifier(const {});
  Timer? _orderingTimer;
  bool _orderingPaused = false;

  /// Seed from disk. NOT fed to [ReadStore.reconcile]: those counts are as old
  /// as the file, and would flag comments as new from last session's numbers.
  Future<void> load() async {
    if (!persists) return;
    final raw = await DiskCache.read(_fileName);
    if (raw is! Map) return;
    raw.forEach((id, v) {
      final e = Map<String, dynamic>.from(v as Map);
      _loaded[id as String] = ScoopMeta.fromJson(Map<String, dynamic>.from(e['meta'] as Map));
      _fetchedAt[id] = DateTime.fromMillisecondsSinceEpoch((e['fetchedAt'] as num).toInt());
    });
    orderingTimes.value = _currentTimes();
  }

  ScoopMeta? of(String id) => _loaded[id];

  /// The thread 404s. Distinct from a failed request.
  bool isUnavailable(String id) => _unavailable.contains(id);

  /// Still waiting on a first answer — the only state that shows a placeholder.
  bool isPending(String id) =>
      _inFlight.contains(id) ||
      (!_loaded.containsKey(id) && !_unavailable.contains(id) && !_failedAt.containsKey(id));

  /// Keep rows in place while the reader is scrolling; card details still
  /// update. An explicit refresh or sort change releases it.
  void setOrderingPaused(bool paused) {
    _orderingPaused = paused;
    if (!paused) _scheduleOrdering();
  }

  /// Mark everything for re-fetch WITHOUT discarding what's on screen (pull to
  /// refresh). The queue is left alone so cards already waiting aren't lost.
  void revalidate() => _stale.addAll(_loaded.keys);

  /// A card is on screen: move it to the head of the queue.
  void ensure(String id) {
    if (!_shouldFetch(id)) return;
    if (_queue.remove(id)) {
      _queue.insert(0, id);
      return;
    }
    _queue.insert(0, id);
    _batchTotal++;
    _pump();
  }

  /// Warm these in the background, behind anything already on screen.
  void prefetch(Iterable<String> ids) {
    for (final id in ids) {
      if (_shouldFetch(id) && !_queue.contains(id)) {
        _queue.add(id);
        _batchTotal++;
      }
    }
    _pump();
  }

  bool _shouldFetch(String id) {
    if (_inFlight.contains(id)) return false;
    // Back off after a failure whatever else we know, so a host that is down
    // isn't retried once per visible row.
    final failed = _failedAt[id];
    if (failed != null && DateTime.now().difference(failed) <= _retryCooldown) return false;
    if (_stale.contains(id)) return true;
    if (_unavailable.contains(id)) return false;
    if (!_loaded.containsKey(id)) return true;
    // Judged live, not only at launch — a session left open all afternoon must
    // not keep showing the morning's reply counts.
    final at = _fetchedAt[id];
    return at == null || DateTime.now().difference(at) > _freshFor;
  }

  void _pump() {
    while (_inFlight.length < _maxInFlight && _queue.isNotEmpty) {
      final id = _queue.removeAt(0);
      if (_shouldFetch(id)) _start(id);
    }
    _publishProgress();
  }

  Future<void> _start(String id) async {
    _inFlight.add(id);
    _stale.remove(id);
    // Cards redraw only when this id's display actually changes. Most refreshes
    // find a thread exactly as it was, and notifying anyway rebuilt every
    // visible card ~75 times per refresh for nothing.
    final hadValue = _loaded.containsKey(id);
    var changed = !hadValue; // a placeholder resolves either way
    try {
      final m = await _fetch(id);
      final meta = ScoopMeta(
        author: m.author,
        authorPoints: m.authorPoints,
        replies: m.replies,
        lastComment: m.lastComment,
      );
      _fetchedAt[id] = DateTime.now();
      _failedAt.remove(id);
      if (_unavailable.remove(id)) changed = true;
      if (_loaded[id] != meta) {
        _loaded[id] = meta;
        changed = true;
        _scheduleOrdering();
      }
      // Saved either way: the timestamp moved even when the values didn't, and
      // it's what the freshness window reads next launch.
      _scheduleSave();
      ReadStore.instance.reconcile(id, m.replies);
    } catch (e) {
      if (e is RotterHttpException && e.isNotFound) {
        if (_unavailable.add(id)) changed = true;
        if (_loaded.remove(id) != null) {
          _scheduleOrdering();
          _scheduleSave();
        }
      } else {
        // Keep whatever was showing — blanking a complete card over one dropped
        // request is exactly what revalidate() works to avoid.
        _failedAt[id] = DateTime.now();
      }
    } finally {
      _inFlight.remove(id);
      _batchDone++;
      if (changed) notifyListeners();
      _pump();
    }
  }

  void _publishProgress() {
    if (_queue.isEmpty && _inFlight.isEmpty) {
      // Forget the batch so the next one starts from empty.
      _batchTotal = 0;
      _batchDone = 0;
      progress.value = null;
    } else if (_batchTotal > 0) {
      progress.value = _batchDone / _batchTotal;
    }
  }

  Map<String, DateTime> _currentTimes() => {
        for (final e in _loaded.entries)
          if (e.value.lastComment != null) e.key: e.value.lastComment!,
      };

  void _scheduleOrdering() {
    if (_orderingPaused || (_orderingTimer?.isActive ?? false)) return;
    _orderingTimer = Timer(const Duration(milliseconds: 600), () {
      if (_orderingPaused) return; // a scroll started while waiting
      final times = _currentTimes();
      if (!mapEquals(times, orderingTimes.value)) orderingTimes.value = times;
    });
  }

  /// Written a couple of seconds after the last arrival, not per arrival.
  void _scheduleSave() {
    if (!persists || (_saveTimer?.isActive ?? false)) return;
    _saveTimer = Timer(const Duration(seconds: 2), () {
      // Loaded entries only, newest first, capped. Failures and 404s are
      // judgements about one moment — persisting them makes rows stick blank.
      final ids = _loaded.keys.toList()
        ..sort((a, b) => (_fetchedAt[b] ?? DateTime(0)).compareTo(_fetchedAt[a] ?? DateTime(0)));
      DiskCache.write(_fileName, {
        for (final id in ids.take(_maxPersisted))
          id: {
            'meta': _loaded[id]!.toJson(),
            'fetchedAt': (_fetchedAt[id] ?? DateTime.now()).millisecondsSinceEpoch,
          },
      });
    });
  }
}
