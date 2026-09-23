import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/scoop.dart';

/// A persisted set of scoops — used twice: bookmarks ([saved]) and threads the
/// user follows ([followed]). Keeps the headline and link, not just the id, so
/// they outlive the rolling RSS feed.
class SavedStore extends ChangeNotifier {
  SavedStore._(this._key);
  static final SavedStore saved = SavedStore._('saved_scoops');
  static final SavedStore followed = SavedStore._('followed_scoops');

  final String _key;
  final Map<String, Scoop> _entries = {};

  Future<void> load() async {
    final raw = (await SharedPreferences.getInstance()).getString(_key);
    if (raw == null) return;
    final map = jsonDecode(raw) as Map<String, dynamic>;
    _entries
      ..clear()
      ..addAll(map.map((k, v) => MapEntry(k, Scoop.fromJson(v as Map<String, dynamic>))));
  }

  List<Scoop> get scoops => _entries.values.toList();
  bool contains(String id) => _entries.containsKey(id);

  Future<void> toggle(Scoop s) async {
    if (_entries.remove(s.id) == null) _entries[s.id] = s;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(_entries.map((k, v) => MapEntry(k, v.toJson()))));
  }
}
