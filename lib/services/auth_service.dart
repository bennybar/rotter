import 'package:flutter/foundation.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Tracks the rotter.net session and holds the saved credentials.
///
/// The session cookie lives in the shared WebView cookie jar (handled by
/// [flutter_inappwebview] so Cloudflare just works). We also persist the
/// username + password in the Keychain/Keystore so the app can sign in silently
/// — the user only re-enters credentials after an explicit sign-out or if the
/// saved ones stop working.
class AuthService {
  AuthService._();
  static final AuthService instance = AuthService._();

  static const _loggedInKey = 'logged_in';
  static const _userKey = 'username';
  static const _secUserKey = 'rotter_user';
  static const _secPassKey = 'rotter_pass';
  static const _secure = FlutterSecureStorage();

  final ValueNotifier<bool> loggedIn = ValueNotifier(false);
  final ValueNotifier<String?> username = ValueNotifier(null);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    loggedIn.value = prefs.getBool(_loggedInKey) ?? false;
    username.value = prefs.getString(_userKey);
  }

  /// The saved credentials, or null if none stored.
  Future<({String user, String pass})?> credentials() async {
    final u = await _secure.read(key: _secUserKey);
    final p = await _secure.read(key: _secPassKey);
    if (u == null || p == null || u.isEmpty || p.isEmpty) return null;
    return (user: u, pass: p);
  }

  Future<bool> get hasCredentials async => (await credentials()) != null;

  Future<void> saveCredentials(String user, String pass) async {
    await _secure.write(key: _secUserKey, value: user);
    await _secure.write(key: _secPassKey, value: pass);
  }

  Future<void> markLoggedIn(String? user) async {
    loggedIn.value = true;
    username.value = user;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_loggedInKey, true);
    if (user != null) await prefs.setString(_userKey, user);
  }

  /// User-initiated sign-out: forget the saved credentials and end the rotter
  /// session by clearing the shared WebView cookie jar (otherwise the next
  /// compose would still post as the previous account).
  Future<void> signOut() async {
    try {
      await CookieManager.instance().deleteAllCookies();
    } catch (_) {/* best-effort */}
    await _secure.delete(key: _secUserKey);
    await _secure.delete(key: _secPassKey);
    await _clearLocal();
  }

  /// Sync to "logged out" when rotter itself shows a logged-out page (session
  /// expired / logged out in the WebView). Does not touch cookies.
  Future<void> markLoggedOut() => _clearLocal();

  Future<void> _clearLocal() async {
    loggedIn.value = false;
    username.value = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_loggedInKey);
    await prefs.remove(_userKey);
  }
}
