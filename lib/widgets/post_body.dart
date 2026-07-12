import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:html/parser.dart' as html_parser;
import 'package:http/http.dart' as http;
import 'package:url_launcher/url_launcher.dart';

import '../services/rotter_ua.dart';
import '../theme.dart';

/// Renders a rotter message body as rich content: inline images, tappable links
/// that open in the right app, **natively rendered Telegram posts**, and lazy
/// video/iframe embeds (YouTube) that only spin up a real WebView when tapped.
///
/// Telegram posts are fetched + parsed and drawn with plain Flutter widgets
/// rather than an inline WebView: a WebView is an Android *platform view*, and
/// putting one inside a scrolling list makes it stutter badly. rotter's own
/// inline styling is stripped upstream, so the body uses the app's typography.
class PostBody extends StatelessWidget {
  final String html;
  final String baseUrl;
  final double fontSize;

  /// False when rendering the text INSIDE a Telegram card, so a t.me link in the
  /// message doesn't recursively embed another Telegram card.
  final bool embedTelegram;

  const PostBody({
    super.key,
    required this.html,
    required this.baseUrl,
    this.fontSize = 16,
    this.embedTelegram = true,
  });

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    return HtmlWidget(
      html,
      baseUrl: Uri.tryParse(baseUrl),
      textStyle: Theme.of(context).textTheme.bodyMedium?.copyWith(
            height: 1.45,
            fontSize: fontSize,
            color: cInk(context),
          ),
      customStylesBuilder: (e) =>
          e.localName == 'a' ? {'color': _hex(accent), 'text-decoration': 'underline'} : null,
      // Auto-embed Telegram post links inline; replace heavy iframe/video embeds
      // with a tap-to-load placeholder.
      customWidgetBuilder: (element) {
        if (embedTelegram && element.localName == 'a') {
          final cp = _telegramChannelPost(element.attributes['href']);
          if (cp != null) return _TelegramCard(channelPost: cp);
        }
        if (element.localName == 'iframe' || element.localName == 'video') {
          final src = element.attributes['src'] ??
              element.querySelector('source')?.attributes['src'];
          if (src != null && src.trim().isNotEmpty) {
            final abs = Uri.tryParse(baseUrl)?.resolve(src).toString() ?? src;
            // A telegram iframe/video duplicates the t.me <a> link we embed
            // above — drop it so the message renders only once.
            if (_telegramChannelPost(abs) != null) return const SizedBox.shrink();
            return _LazyEmbed(url: abs);
          }
        }
        return null;
      },
      onTapUrl: (url) async {
        final uri = Uri.tryParse(url);
        if (uri == null) return false;
        return launchUrl(uri, mode: LaunchMode.externalApplication);
      },
      // Tap an inline image → full-screen, pinch-to-zoom viewer.
      onTapImage: (img) {
        final url = img.sources.isNotEmpty ? img.sources.first.url : null;
        if (url != null) {
          Navigator.of(context).push(PageRouteBuilder(
            opaque: false,
            barrierColor: Colors.black,
            pageBuilder: (_, __, ___) => _ImageViewer(url: url),
          ));
        }
      },
    );
  }

  String _hex(Color c) =>
      '#${(c.toARGB32() & 0xFFFFFF).toRadixString(16).padLeft(6, '0')}';
}

/// Full-screen, pinch-to-zoom image viewer with tap/swipe-down to dismiss.
class _ImageViewer extends StatelessWidget {
  final String url;
  const _ImageViewer({required this.url});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        children: [
          GestureDetector(
            onTap: () => Navigator.of(context).pop(),
            onVerticalDragEnd: (d) {
              if ((d.primaryVelocity ?? 0).abs() > 300) Navigator.of(context).pop();
            },
            child: InteractiveViewer(
              minScale: 1,
              maxScale: 5,
              child: Center(child: Image.network(url)),
            ),
          ),
          PositionedDirectional(
            top: MediaQuery.of(context).padding.top + 8,
            end: 8,
            child: IconButton(
              icon: const Icon(Icons.close_rounded, color: Colors.white, size: 30),
              onPressed: () => Navigator.of(context).pop(),
            ),
          ),
        ],
      ),
    );
  }
}

/// For a Telegram post link `t.me/<channel>/<id>` (id numeric) returns
/// "channel/id"; null for anything else (channel-only links, /s/ previews, …).
String? _telegramChannelPost(String? url) {
  if (url == null) return null;
  final u = Uri.tryParse(url.trim());
  if (u == null) return null;
  final host = u.host.replaceFirst('www.', '');
  if (host != 't.me' && host != 'telegram.me') return null;
  final segs = u.pathSegments.where((s) => s.isNotEmpty).toList();
  if (segs.length >= 2 && segs.first != 's' && RegExp(r'^\d+$').hasMatch(segs.last)) {
    return '${segs[segs.length - 2]}/${segs.last}';
  }
  return null;
}

/// A Telegram post scraped from its public `?embed=1` page.
class _TelegramPost {
  final String channel;
  final String textHtml;
  final String? photoUrl;
  const _TelegramPost({required this.channel, required this.textHtml, this.photoUrl});
}

/// Fetched posts, kept for the app's lifetime so scrolling never re-fetches.
/// A cached `null` means "we tried and it isn't available" (don't retry).
final Map<String, _TelegramPost?> _tgCache = {};

Future<_TelegramPost?> _fetchTelegram(String channelPost) async {
  if (_tgCache.containsKey(channelPost)) return _tgCache[channelPost];
  try {
    final r = await http
        .get(Uri.parse('https://t.me/$channelPost?embed=1'),
            headers: const {'User-Agent': kRotterUserAgent})
        .timeout(const Duration(seconds: 12));
    if (r.statusCode != 200) return _tgCache[channelPost] = null;
    final doc = html_parser.parse(utf8.decode(r.bodyBytes));
    final text = doc.querySelector('.tgme_widget_message_text')?.innerHtml ?? '';
    final channel = doc.querySelector('.tgme_widget_message_owner_name')?.text.trim() ??
        channelPost.split('/').first;
    final style = doc.querySelector('.tgme_widget_message_photo_wrap')?.attributes['style'];
    final photo = style == null
        ? null
        : RegExp(r"background-image:\s*url\('([^']+)'\)").firstMatch(style)?.group(1);
    if (text.isEmpty && photo == null) return _tgCache[channelPost] = null;
    return _tgCache[channelPost] =
        _TelegramPost(channel: channel, textHtml: text, photoUrl: photo);
  } catch (_) {
    return _tgCache[channelPost] = null;
  }
}

/// A Telegram post rendered with NATIVE widgets (no WebView — a WebView here is
/// an Android platform view and makes the list stutter). Tapping opens Telegram.
class _TelegramCard extends StatefulWidget {
  final String channelPost; // "channel/id"
  const _TelegramCard({required this.channelPost});

  @override
  State<_TelegramCard> createState() => _TelegramCardState();
}

class _TelegramCardState extends State<_TelegramCard> {
  late Future<_TelegramPost?> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetchTelegram(widget.channelPost);
  }

  void _open() {
    final uri = Uri.tryParse('https://t.me/${widget.channelPost}');
    if (uri != null) launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  @override
  Widget build(BuildContext context) {
    const tg = Color(0xFF2AABEE);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: FutureBuilder<_TelegramPost?>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return Container(
              height: 72,
              decoration: BoxDecoration(
                color: cField(context),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Center(
                child: SizedBox(
                    width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
              ),
            );
          }
          final post = snap.data;
          // Couldn't load → just a plain link out to Telegram.
          if (post == null) {
            return InkWell(
              onTap: _open,
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.send_rounded, size: 16, color: tg),
                const SizedBox(width: 6),
                Flexible(
                  child: Text('t.me/${widget.channelPost}',
                      style: const TextStyle(
                          color: tg, decoration: TextDecoration.underline, fontSize: 14)),
                ),
              ]),
            );
          }
          return InkWell(
            onTap: _open,
            borderRadius: BorderRadius.circular(14),
            child: Container(
              decoration: BoxDecoration(
                color: cField(context),
                borderRadius: BorderRadius.circular(14),
                border: BorderDirectional(start: BorderSide(color: tg, width: 3)),
              ),
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    const Icon(Icons.send_rounded, size: 15, color: tg),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(post.channel,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                              fontWeight: FontWeight.w800, fontSize: 13, color: tg)),
                    ),
                  ]),
                  if (post.photoUrl != null) ...[
                    const SizedBox(height: 8),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: Image.network(
                        post.photoUrl!,
                        fit: BoxFit.cover,
                        // Don't jump the layout while the photo streams in.
                        loadingBuilder: (context, child, progress) => progress == null
                            ? child
                            : Container(height: 180, color: cMuted(context).withValues(alpha: 0.08)),
                        errorBuilder: (_, __, ___) => const SizedBox.shrink(),
                      ),
                    ),
                  ],
                  if (post.textHtml.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    // embedTelegram:false → a t.me link inside the message won't
                    // recursively spawn another card.
                    PostBody(
                      html: post.textHtml,
                      baseUrl: 'https://t.me/',
                      fontSize: 14.5,
                      embedTelegram: false,
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

/// True for YouTube watch/share/shorts/embed URLs.
bool _isYouTube(String url) {
  final h = Uri.tryParse(url)?.host.replaceFirst('www.', '') ?? '';
  return h == 'youtu.be' || h.endsWith('youtube.com') || h.endsWith('youtube-nocookie.com');
}

/// Normalize a YouTube URL to its embeddable `/embed/<id>` form (watch URLs are
/// refused inside an iframe; only `/embed/` plays).
String _normalizeEmbedUrl(String url) {
  final u = Uri.tryParse(url);
  if (u == null) return url;
  final host = u.host.replaceFirst('www.', '');
  String? id;
  if (host == 'youtu.be') {
    id = u.pathSegments.isNotEmpty ? u.pathSegments.first : null;
  } else if (host.endsWith('youtube.com') || host.endsWith('youtube-nocookie.com')) {
    if (u.pathSegments.isNotEmpty && u.pathSegments.first == 'embed') return url;
    id = u.queryParameters['v'];
    if (id == null && u.pathSegments.length >= 2 && u.pathSegments.first == 'shorts') {
      id = u.pathSegments[1];
    }
  }
  return id == null ? url : 'https://www.youtube.com/embed/$id?playsinline=1&rel=0';
}

/// A media embed that shows a lightweight placeholder until tapped; only then
/// does it create a real inline WebView (so a thread full of embeds stays light).
class _LazyEmbed extends StatefulWidget {
  final String url;
  const _LazyEmbed({required this.url});

  @override
  State<_LazyEmbed> createState() => _LazyEmbedState();
}

class _LazyEmbedState extends State<_LazyEmbed> {
  bool _loaded = false;

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    final youtube = _isYouTube(widget.url);
    final src = youtube ? _normalizeEmbedUrl(widget.url) : widget.url;
    final host = Uri.tryParse(widget.url)?.host.replaceFirst('www.', '') ?? '';

    // fwfh lays the custom widget out shrink-wrapped, so size it explicitly:
    // 16:9 for video, a taller card for social (Telegram) embeds.
    final width = (MediaQuery.sizeOf(context).width - 64).clamp(220.0, 720.0);
    final height = youtube ? width * 9 / 16 : 460.0;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(14),
        child: SizedBox(
          width: width,
          height: height,
          child: _loaded
              ? InAppWebView(
                  initialUrlRequest: URLRequest(url: WebUri(src)),
                  initialSettings: InAppWebViewSettings(
                    transparentBackground: true,
                    allowsInlineMediaPlayback: true,
                    mediaPlaybackRequiresUserGesture: false,
                  ),
                )
              : Material(
                  color: cField(context),
                  child: InkWell(
                    onTap: () => setState(() => _loaded = true),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(color: accent, shape: BoxShape.circle),
                          child: const Icon(Icons.play_arrow_rounded, color: Colors.white, size: 34),
                        ),
                        const SizedBox(height: 12),
                        Text(host,
                            style: TextStyle(
                                color: cMuted(context), fontWeight: FontWeight.w700, fontSize: 13)),
                      ],
                    ),
                  ),
                ),
        ),
      ),
    );
  }
}
