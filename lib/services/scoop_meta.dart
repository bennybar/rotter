import 'package:flutter/foundation.dart';

import 'rotter_service.dart';

class ScoopMeta {
  final String? author;
  final int? replies;
  final DateTime? lastComment;
  final bool unavailable; // thread 404s — removed, or not generated yet
  const ScoopMeta({this.author, this.replies, this.lastComment, this.unavailable = false});
}

/// Lazily fetches and caches per-card metadata (author + reply count) for scoops.
/// Cards call [ensure] when they scroll into view; results are cached in memory
/// and a small in-flight cap keeps it from bursting dozens of requests at once.
class ScoopMetaCache extends ChangeNotifier {
  ScoopMetaCache._();
  static final ScoopMetaCache instance = ScoopMetaCache._();

  static const _maxInFlight = 6;
  final Map<String, ScoopMeta> _cache = {};
  final Set<String> _inFlight = {};

  ScoopMeta? of(String id) => _cache[id];

  /// Drop all cached metadata so visible cards re-fetch fresh reply counts +
  /// last-comment times (used on pull-to-refresh). In-flight fetches are left to
  /// complete and will simply repopulate.
  void invalidate() {
    _cache.clear();
    notifyListeners();
  }

  /// Kick off a fetch if this id isn't cached or already loading. Safe to call
  /// from build — it dedupes and notifies only when data arrives.
  void ensure(String id) {
    if (_cache.containsKey(id) || _inFlight.contains(id)) return;
    if (_inFlight.length >= _maxInFlight) return; // retried on next scroll/build
    _inFlight.add(id);
    RotterService.instance.fetchCardMeta(id).then((m) {
      _cache[id] = ScoopMeta(author: m.author, replies: m.replies, lastComment: m.lastComment);
    }).catchError((e) {
      // Cache the result so we don't hammer a failing thread every frame. A 404
      // means the thread doesn't exist (removed, or not generated yet).
      _cache[id] = ScoopMeta(unavailable: e.toString().contains('404'));
    }).whenComplete(() {
      _inFlight.remove(id);
      notifyListeners();
    });
  }
}
