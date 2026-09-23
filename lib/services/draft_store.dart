import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Unsent composer text, kept per target (a reply, an edit, or a new thread) so
/// nothing typed is lost to a back-swipe or a failed post. Saved as it's typed,
/// restored when the same target is opened again, cleared once the post
/// succeeds or the user explicitly discards it.
class DraftStore {
  DraftStore._();
  static final DraftStore instance = DraftStore._();

  static const _key = 'composer_drafts';
  // { target: {"subject": ..., "body": ...} }
  final Map<String, Map<String, String>> _drafts = {};

  Future<void> load() async {
    final raw = (await SharedPreferences.getInstance()).getString(_key);
    if (raw == null) return;
    final map = jsonDecode(raw) as Map<String, dynamic>;
    _drafts
      ..clear()
      ..addAll(map.map((k, v) => MapEntry(k, (v as Map<String, dynamic>).cast<String, String>())));
  }

  ({String subject, String body})? draft(String target) {
    final d = _drafts[target];
    return d == null ? null : (subject: d['subject'] ?? '', body: d['body'] ?? '');
  }

  Future<void> save(String target, String subject, String body) async {
    // An emptied composer is not a draft worth restoring.
    if (subject.trim().isEmpty && body.trim().isEmpty) return clear(target);
    final d = _drafts[target];
    if (d != null && d['subject'] == subject && d['body'] == body) return;
    _drafts[target] = {'subject': subject, 'body': body};
    await _save();
  }

  Future<void> clear(String target) async {
    if (_drafts.remove(target) != null) await _save();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(_drafts));
  }
}
