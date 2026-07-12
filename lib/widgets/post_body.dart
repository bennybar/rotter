import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:url_launcher/url_launcher.dart';

import '../theme.dart';

/// Renders a rotter message body as rich content: inline images, tappable links
/// that open in the right app, and **lazy** video/iframe embeds (YouTube /
/// Telegram) that only spin up a real WebView when the user taps to load — so a
/// thread with many embeds stays light and scrolls smoothly. rotter's own inline
/// styling is stripped upstream, so the body uses the app's typography here.
class PostBody extends StatelessWidget {
  final String html;
  final String baseUrl;
  final double fontSize;

  const PostBody({
    super.key,
    required this.html,
    required this.baseUrl,
    this.fontSize = 16,
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
        if (element.localName == 'a') {
          final cp = _telegramChannelPost(element.attributes['href']);
          if (cp != null) return _TelegramEmbed(channelPost: cp);
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

/// Inline Telegram post, loaded automatically via Telegram's own `?embed=1`
/// page and auto-sized to the message height (no tap needed). Disabled inner
/// scrolling so it behaves as a static block inside the thread list.
class _TelegramEmbed extends StatefulWidget {
  final String channelPost; // "channel/id"
  const _TelegramEmbed({required this.channelPost});

  @override
  State<_TelegramEmbed> createState() => _TelegramEmbedState();
}

class _TelegramEmbedState extends State<_TelegramEmbed> {
  double _height = 160;

  @override
  Widget build(BuildContext context) {
    final width = (MediaQuery.sizeOf(context).width - 64).clamp(220.0, 720.0);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(14),
        child: SizedBox(
          width: width,
          height: _height,
          child: InAppWebView(
            initialUrlRequest:
                URLRequest(url: WebUri('https://t.me/${widget.channelPost}?embed=1')),
            initialSettings: InAppWebViewSettings(
              transparentBackground: true,
              supportZoom: false,
              disableVerticalScroll: true,
              disableHorizontalScroll: true,
            ),
            onContentSizeChanged: (controller, oldSize, newSize) {
              final h = newSize.height;
              if (h > 40 && (h - _height).abs() > 2 && mounted) {
                setState(() => _height = h.clamp(80.0, 1400.0));
              }
            },
          ),
        ),
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
