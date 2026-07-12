import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Tracks which scoop threads have been read.
///
/// For each read thread we remember the **reply count we last saw**. A thread is
/// "read" while its current reply count is still ≤ the seen count; once new
/// comments arrive (current > seen) it flips back to unread *and* is flagged
/// [isNew] so the list can show a "new comments" pill. This is the source of
/// truth for dulling read cards.
class ReadStore extends ChangeNotifier {
  ReadStore._();
  static final ReadStore instance = ReadStore._();

  static const _key = 'read_threads'; // JSON: { id: seenReplyCount }
  static const _newKey = 'new_threads'; // JSON: [ ids with new comments ]

  /// Sentinel: "read, but the reply-count baseline isn't known yet" (e.g. marked
  /// read via swipe while offline). Stays read; the next sweep sets the real
  /// baseline. Prevents a failed count fetch from leaving the thread stuck.
  static const int pending = -1;

  final Map<String, int> _seen = {};
  final Set<String> _new = {};

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw != null) {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      _seen
        ..clear()
        ..addAll(map.map((k, v) => MapEntry(k, (v as num).toInt())));
    }
    final rawNew = prefs.getString(_newKey);
    if (rawNew != null) {
      _new
        ..clear()
        ..addAll((jsonDecode(rawNew) as List).cast<String>());
    }
  }

  bool isRead(String id) => _seen.containsKey(id);
  int? seenCount(String id) => _seen[id];
  bool isPending(String id) => _seen[id] == pending;

  /// True for threads that were read and have since gained new comments.
  bool isNew(String id) => _new.contains(id);

  Future<void> markRead(String id, int replyCount) async {
    _seen[id] = replyCount;
    _new.remove(id); // opening/reading clears the "new" flag
    notifyListeners();
    await _save();
  }

  /// Mark every given thread read at once (the "mark all read" action). Threads
  /// whose baseline we don't know are set [pending] — the next sweep fills in the
  /// real reply count without flagging them new.
  Future<void> markAllRead(Iterable<String> ids) async {
    var changed = false;
    for (final id in ids) {
      if (!_seen.containsKey(id)) {
        _seen[id] = pending;
        changed = true;
      }
      if (_new.remove(id)) changed = true;
    }
    if (changed) {
      notifyListeners();
      await _save();
    }
  }

  Future<void> markUnread(String id) async {
    final hadSeen = _seen.remove(id) != null;
    final hadNew = _new.remove(id);
    if (hadSeen || hadNew) {
      notifyListeners();
      await _save();
    }
  }

  /// Sweep found new comments on a previously-read thread: drop it back to
  /// unread and flag it "new".
  Future<void> markNewComments(String id) async {
    _seen.remove(id);
    _new.add(id);
    notifyListeners();
    await _save();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(_seen));
    await prefs.setString(_newKey, jsonEncode(_new.toList()));
  }
}
