import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Per-thread reading history: the highest message number seen (so the next
/// visit can mark newer comments "new") and where the reader left off (so the
/// thread can offer to resume there). Message numbers only, never bodies.
class ReadingStore {
  ReadingStore._();
  static final ReadingStore instance = ReadingStore._();

  static const _key = 'thread_reading_history';
  // { id: {"latest": int?, "position": int?} }
  final Map<String, Map<String, int>> _visits = {};

  Future<void> load() async {
    final raw = (await SharedPreferences.getInstance()).getString(_key);
    if (raw == null) return;
    final map = jsonDecode(raw) as Map<String, dynamic>;
    _visits
      ..clear()
      ..addAll(map.map((k, v) => MapEntry(
          k, (v as Map<String, dynamic>).map((f, n) => MapEntry(f, (n as num).toInt())))));
  }

  int? latestMessage(String id) => _visits[id]?['latest'];
  int? position(String id) => _visits[id]?['position'];

  Future<void> recordVisit(String id, int latestMessage) async {
    final v = _visits.putIfAbsent(id, () => {});
    final prev = v['latest'] ?? 0;
    v['latest'] = latestMessage > prev ? latestMessage : prev;
    await _save();
  }

  Future<void> rememberPosition(String id, int number) async {
    if (_visits[id]?['position'] == number) return;
    _visits.putIfAbsent(id, () => {})['position'] = number;
    await _save();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(_visits));
  }
}
