/// One User-Agent used EVERYWHERE we talk to rotter — the webviews (login,
/// embeds, gated pages) and the direct HTTP calls (post/reply). Cloudflare ties
/// the `cf_clearance` cookie to the UA that earned it, so a mismatch between the
/// webview that cleared Cloudflare and the http client that reuses the cookie
/// would get 403'd. Keep them identical.
const String kRotterUserAgent =
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 '
    '(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1';
