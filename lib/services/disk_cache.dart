import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// A small JSON file under the app's cache directory, for data the app can
/// always refetch (the last feed, per-card metadata).
///
/// Not SharedPreferences, where the rest of the app's state lives: that is
/// loaded whole into memory at launch, the wrong home for ~100KB of metadata
/// that only exists to save a network round trip. The cache directory is also
/// the one the OS may purge under storage pressure — right for regenerable data.
class DiskCache {
  DiskCache._();

  static Future<Object?> read(String name) async {
    try {
      final f = await _file(name);
      if (!await f.exists()) return null;
      return jsonDecode(await f.readAsString());
    } catch (_) {
      return null; // missing / corrupt → just refetch
    }
  }

  static Future<void> write(String name, Object value) async {
    try {
      final f = await _file(name);
      final tmp = File('${f.path}.tmp');
      await tmp.writeAsString(jsonEncode(value), flush: true);
      await tmp.rename(f.path); // atomic replace
    } catch (_) {/* best-effort */}
  }

  static Future<File> _file(String name) async =>
      File('${(await getApplicationCacheDirectory()).path}/$name');
}
