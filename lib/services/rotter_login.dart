import 'package:flutter/foundation.dart';

import 'auth_service.dart';
import 'rotter_gated.dart';
import 'win1255.dart';

enum LoginOutcome {
  success,
  wrongCredentials,
  // Couldn't complete (no Cloudflare clearance, network error). Caller shows an
  // error — there is no webview fallback.
  failed,
}

class LoginResult {
  final LoginOutcome outcome;
  final String? user;
  const LoginResult(this.outcome, [this.user]);
}

/// Signs in to rotter.net the way the Android app does: a **direct cp1255 POST**
/// to `dcboard.cgi` (`cmd=login&az=login` plus the Hebrew username/password
/// fields), NOT by driving rotter's web login page (which redirects to the
/// homepage / is a modern SPA). The request is executed through [RotterGated] so
/// it runs inside the Cloudflare-cleared browser context. The session cookie the
/// login sets lands in the shared WebView cookie jar that posting reuses.
class RotterLogin {
  RotterLogin._();

  static const _base = 'https://rotter.net/cgi-bin/forum/dcboard.cgi';

  /// Attempt a sign-in. On success, persists the session + username (credentials
  /// are NOT saved here — the caller decides whether to keep them).
  static Future<LoginResult> attempt({required String user, required String pass}) async {
    // 1. Direct login POST — cp1255 form with the Hebrew field names the app uses.
    final body = encodeWin1255Form({
      'cmd': 'login',
      'az': 'login',
      'שם-משתמש': user,
      'סיסמא': pass,
    });
    final resp = await RotterGated.request(_base, method: 'POST', body: body);
    if (resp == null || resp.status == 403) return const LoginResult(LoginOutcome.failed);
    final post = resp.text.toLowerCase();
    debugPrint('RotterLogin POST: status=${resp.status} len=${post.length} '
        'logout=${post.contains('az=logout')} pw=${post.contains('type=password')}');
    // The login response (after following its redirect) shows the logout link
    // when sign-in succeeded.
    if (post.contains('az=logout')) {
      await AuthService.instance.markLoggedIn(user);
      return LoginResult(LoginOutcome.success, user);
    }

    // 2. Fallback verify on the compose page (same persistent webview → the
    // session cookie login set is present). Logged in iff it shows the logout
    // link or the compose textarea.
    final verify = await RotterGated.request('$_base?az=post&forum=scoops1');
    if (verify == null) return const LoginResult(LoginOutcome.failed);
    final html = verify.text.toLowerCase();
    debugPrint('RotterLogin VERIFY: status=${verify.status} len=${html.length} '
        'logout=${html.contains('az=logout')} textarea=${html.contains('<textarea')} '
        'pw=${html.contains('type=password')}');
    if (html.contains('az=logout') || html.contains('<textarea')) {
      await AuthService.instance.markLoggedIn(user);
      return LoginResult(LoginOutcome.success, user);
    }
    return const LoginResult(LoginOutcome.wrongCredentials);
  }

  /// Best-effort silent refresh at startup: if credentials are stored, sign in
  /// again so the session cookie is fresh. Wrong saved credentials clear the
  /// logged-in flag (but keep the creds so the user can fix them); anything else
  /// (offline, Cloudflare) leaves the last-known state untouched.
  static Future<void> refreshSession() async {
    final creds = await AuthService.instance.credentials();
    if (creds == null) return;
    final r = await attempt(user: creds.user, pass: creds.pass);
    if (r.outcome == LoginOutcome.wrongCredentials) {
      await AuthService.instance.markLoggedOut();
    }
  }
}
