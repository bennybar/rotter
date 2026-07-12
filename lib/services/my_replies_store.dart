import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Tracks the scoop threads the user has replied to (persisted), so the list and
/// the thread can mark them with a green side. Which *comment* is the user's own
/// is decided live by matching the author to the logged-in username; this store
/// only answers "did I reply in this thread?" for the list, where we can't see
/// the comments without loading each thread.
class MyRepliesStore extends ChangeNotifier {
  MyRepliesStore._();
  static final MyRepliesStore instance = MyRepliesStore._();

  static const _key = 'my_reply_threads';
  final Set<String> _threads = {};

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw != null) {
      _threads
        ..clear()
        ..addAll((jsonDecode(raw) as List).cast<String>());
    }
  }

  bool replied(String threadId) => _threads.contains(threadId);

  Future<void> add(String threadId) async {
    if (_threads.add(threadId)) {
      notifyListeners();
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(_threads.toList()));
    }
  }
}
