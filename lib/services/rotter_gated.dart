import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import 'rotter_ua.dart';
import 'win1255.dart';

/// Response from a gated dcboard.cgi request: HTTP status, whether the request
/// followed a redirect (rotter returns 302 → the thread on a successful post),
/// and the raw cp1255 bytes.
class GatedResponse {
  final int status;
  final bool redirected;
  final List<int> bytes;
  const GatedResponse(this.status, this.redirected, this.bytes);
  String get text => decodeWin1255(bytes);
}

/// Runs gated `dcboard.cgi` requests (login / post / reply) the way the Android
/// app does *semantically* — direct dcboard.cgi calls with windows-1255
/// urlencoded bodies — but EXECUTES them inside a headless WebView via `fetch()`.
///
/// The APK uses OkHttp + a stored `cf_clearance` cookie. On iOS, replaying that
/// cookie from a raw Dart HTTP client fails (Cloudflare TLS / HttpOnly cookie).
/// Running the request from the page's own `fetch()` means it goes out in the
/// exact browser context that already cleared Cloudflare — same cookies, same
/// TLS. A SINGLE persistent webview is reused for every call so the session
/// cookie login sets is visible to the verify + post requests that follow.
class RotterGated {
  RotterGated._();

  static const _base = 'https://rotter.net/cgi-bin/forum/dcboard.cgi';
  static const _boot = '$_base?az=login';

  static HeadlessInAppWebView? _hw;
  static InAppWebViewController? _ctrl;
  static Future<bool>? _booting;

  /// Boot (once) a headless webview parked on a gated rotter page, so its JS
  /// context has cleared Cloudflare and holds the cookie jar. Returns false if it
  /// can't come up.
  static Future<bool> _ensure() async {
    if (_ctrl != null) return true;
    if (_booting != null) return _booting!;
    final ready = Completer<bool>();
    _booting = ready.future;

    _hw = HeadlessInAppWebView(
      initialUrlRequest: URLRequest(url: WebUri(_boot)),
      initialSettings: InAppWebViewSettings(userAgent: kRotterUserAgent, clearCache: false),
      onWebViewCreated: (c) => _ctrl = c,
      onLoadStop: (c, _) async {
        _ctrl = c;
        final title = (await c.getTitle())?.toLowerCase() ?? '';
        // Cloudflare interstitial still up — wait for it to redirect to the page.
        if (title.contains('just a moment') || title.contains('attention required')) return;
        if (!ready.isCompleted) ready.complete(true);
      },
    );
    Timer(const Duration(seconds: 40), () {
      if (!ready.isCompleted) ready.complete(_ctrl != null);
    });
    await _hw!.run();
    final ok = await ready.future;
    _booting = null;
    return ok;
  }

  /// GET or POST [url]. [body], when non-null, is an already %-encoded cp1255
  /// urlencoded form string, sent as `application/x-www-form-urlencoded`.
  static Future<GatedResponse?> request(String url, {String method = 'GET', String? body}) async {
    if (!await _ensure() || _ctrl == null) {
      debugPrint('RotterGated: webview not ready');
      return null;
    }
    try {
      // callAsyncJavaScript (NOT evaluateJavascript) awaits the fetch promise.
      final res = await _ctrl!.callAsyncJavaScript(
        functionBody: _fetchBody,
        arguments: {'u': url, 'm': method, 'bd': body},
      );
      final value = res?.value;
      if (res?.error != null || value is! Map) {
        debugPrint('RotterGated fetch error: ${res?.error} for $method $url');
        return null;
      }
      final m = Map<String, dynamic>.from(value);
      return GatedResponse(
        (m['status'] as num).toInt(),
        m['redirected'] == true,
        base64Decode(m['b64'] as String? ?? ''),
      );
    } catch (e) {
      debugPrint('RotterGated fetch threw: $e');
      return null;
    }
  }

  // Body of an async function (args: u=url, m=method, bd=body). fetch() inside the
  // page → {status, body-as-base64-of-raw-bytes}. We read arrayBuffer (not text)
  // so cp1255 bytes survive to be decoded in Dart.
  static const _fetchBody = '''
    var opts = {method: m, credentials: 'include', redirect: 'follow'};
    if (bd !== null) {
      opts.headers = {'Content-Type': 'application/x-www-form-urlencoded'};
      opts.body = bd;
    }
    var r = await fetch(u, opts);
    var buf = await r.arrayBuffer();
    var bytes = new Uint8Array(buf), bin = '';
    for (var i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
    return {status: r.status, redirected: r.redirected, b64: btoa(bin)};
  ''';
}
