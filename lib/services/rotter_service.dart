import 'dart:isolate';

import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:http/http.dart' as http;
import 'package:xml/xml.dart';

import '../models/message.dart';
import '../models/scoop.dart';
import 'win1255.dart';

/// Reads rotter.net the browserless way: RSS for the list, `.shtml` for threads.
/// Everything on rotter is windows-1255 — we always decode raw bytes ourselves.
class RotterService {
  RotterService._();
  static final RotterService instance = RotterService._();

  static const _rssUrl = 'https://rotter.net/rss/rotternews.xml';
  static String threadUrl(String id) => 'https://rotter.net/forum/scoops1/$id.shtml';

  static const _timeout = Duration(seconds: 15);

  final _client = http.Client();

  Future<List<int>> _getBytes(String url) async {
    final res = await _client.get(Uri.parse(url)).timeout(_timeout);
    if (res.statusCode != 200) throw RotterHttpException(res.statusCode, url);
    return res.bodyBytes;
  }

  // ---- Post list (RSS) ----------------------------------------------------

  // Decode (cp1255) + parse in a background isolate, like card meta: a thread
  // page is 100–400KB and building its DOM on the UI isolate drops frames.
  // `RotterService.instance` inside the closure is that isolate's own copy.
  Future<List<Scoop>> fetchScoops() async {
    final bytes = await _getBytes(_rssUrl);
    return Isolate.run(() => RotterService.instance.parseRss(decodeWin1255(bytes)));
  }

  /// Pure RSS → scoops parse (network-free; unit-testable).
  List<Scoop> parseRss(String xml) {
    final doc = XmlDocument.parse(xml);
    final out = <Scoop>[];
    for (final it in doc.findAllElements('item')) {
      final link = it.getElement('link')?.innerText.trim() ?? '';
      final id = RegExp(r'/(\d+)\.shtml').firstMatch(link)?.group(1);
      if (id == null) continue;
      out.add(Scoop(
        id: id,
        // rotter escapes ';' as '\;' in RSS titles — unescape it.
        title: _decodeEntities(it.getElement('title')?.innerText.trim() ?? '')
            .replaceAll(r'\;', ';'),
        url: link,
        published: _parseRfc822(it.getElement('pubDate')?.innerText.trim()),
      ));
    }
    return out;
  }

  /// Lightweight per-card metadata (root author + their points, reply count,
  /// last-comment time) for list cards — the RSS doesn't carry these. The
  /// last-comment time powers the "sort by last comment" option. Browserless:
  /// one `.shtml` fetch.
  Future<CardMeta> fetchCardMeta(String id) async {
    final url = threadUrl(id);
    final res = await _client.get(Uri.parse(url)).timeout(_timeout);
    if (res.statusCode != 200) throw RotterHttpException(res.statusCode, url);
    final bytes = res.bodyBytes;
    // Decode (cp1255) + scan the 100KB page OFF the main isolate, otherwise each
    // card drops frames and the list scrolls choppily.
    return Isolate.run(() => parseCardMeta(decodeWin1255(bytes)));
  }

  // ---- Thread + comments (.shtml) -----------------------------------------

  Future<Thread> fetchThread(String id) async {
    final bytes = await _getBytes(threadUrl(id));
    return Isolate.run(() => RotterService.instance.parseThread(decodeWin1255(bytes), id));
  }

  /// Pure thread-HTML → tree parse (network-free; unit-testable).
  Thread parseThread(String html, String id) {
    final doc = html_parser.parse(html);
    final messages = <Message>[];

    for (final a in doc.querySelectorAll('a[name]')) {
      final name = a.attributes['name'];
      if (name == null || !RegExp(r'^\d+$').hasMatch(name)) continue;

      final table = _ancestorTable(a);
      if (table == null) continue;

      final replyTo = table.querySelector('a[href^="#"]');
      final (date, time) = _dateTime(table);
      final th = table.innerHtml;
      messages.add(Message(
        num: int.parse(name),
        author: (a.querySelector('b')?.text ?? a.text).trim(),
        rating: int.tryParse(RegExp(r'(\d)_star').firstMatch(th)?.group(1) ?? ''),
        // The star image links to the member's ratings/details page — keep it so
        // tapping the author can open it (already correctly cp1255-encoded).
        profileUrl: table.querySelector('a[href*="view_user_ratings"]')?.attributes['href'],
        // Member stats shown next to the author in the thread HTML.
        joinDate: RegExp(r'חבר מתאריך\s*([\d.]+)').firstMatch(th)?.group(1),
        messages: parseStat('הודעות', th),
        raters: parseStat('מדרגים', th),
        points: parseStat('נקודות', th),
        title: _title(table, isRoot: name == '0'),
        bodyHtml: _bodyHtml(table),
        date: date,
        time: time,
        timestamp: commentTimestamp(date, time),
        parent: replyTo == null
            ? null
            : int.tryParse(replyTo.attributes['href']!.replaceAll('#', '')),
      ));
    }

    // De-dup (anchors can repeat) keeping first seen, then order by num.
    final seen = <int>{};
    final unique = messages.where((m) => seen.add(m.num)).toList()
      ..sort((a, b) => a.num.compareTo(b.num));
    return Thread(id: id, messages: unique);
  }

  /// Parse a comment's gregorian `DD.MM.YY` + `HH:MM` into a local DateTime.
  /// (The root's date is a Hebrew calendar string and isn't parsed here — the
  /// reader falls back to the RSS pubDate for the root.)
  static DateTime? commentTimestamp(String? date, String? time) {
    if (date == null) return null;
    final d = RegExp(r'^(\d{1,2})\.(\d{1,2})\.(\d{2,4})$').firstMatch(date.trim());
    if (d == null) return null;
    final t = time == null
        ? null
        : RegExp(r'^(\d{1,2}):(\d{2})$').firstMatch(time.trim());
    var year = int.parse(d.group(3)!);
    if (year < 100) year += 2000;
    try {
      return DateTime(
        year,
        int.parse(d.group(2)!),
        int.parse(d.group(1)!),
        t == null ? 0 : int.parse(t.group(1)!),
        t == null ? 0 : int.parse(t.group(2)!),
      );
    } catch (_) {
      return null;
    }
  }

  static dom.Element? _ancestorTable(dom.Element e) {
    dom.Element? cur = e.parent;
    while (cur != null) {
      if (cur.localName == 'table') return cur;
      cur = cur.parent;
    }
    return null;
  }

  String? _title(dom.Element table, {required bool isRoot}) {
    final raw = table.querySelector(isRoot ? 'h1.text16b' : '.text16b')?.text.trim();
    if (raw == null || raw.isEmpty) return null;
    // Comment titles are prefixed "N. " — drop the leading message number.
    return raw.replaceFirst(RegExp(r'^\d+\.\s*'), '').trim();
  }

  /// The post date + time live in the header cell: the time is a red
  /// `<font color="red">HH:MM</font>` (NOT the red "feedback" link, which also
  /// exists), and the date is the gregorian DD.MM.YY in the same cell (falling
  /// back to the text right before the time when there is none).
  static (String?, String?) _dateTime(dom.Element table) {
    dom.Element? timeEl;
    for (final f in table.querySelectorAll('font[color="red"]')) {
      if (RegExp(r'^\d{1,2}:\d{2}$').hasMatch(f.text.trim())) {
        timeEl = f;
        break;
      }
    }
    if (timeEl == null) return (null, null);
    final time = timeEl.text.trim();
    // Every header — root and comments alike — reads Hebrew date, time, then
    // the gregorian date, so "the text before the time" is always the Hebrew
    // date (which doesn't parse: every comment had a null timestamp and
    // last-comment sort had nothing to sort on). Take the gregorian DD.MM.YY
    // from anywhere in the enclosing <td> instead — that scope excludes the
    // author's join date, which sits in the neighbouring cell.
    dom.Element? cell = timeEl.parent;
    while (cell != null && cell.localName != 'td' && cell.localName != 'th') {
      cell = cell.parent;
    }
    final gregorian =
        RegExp(r'\d{1,2}\.\d{1,2}\.\d{2,4}').firstMatch(cell?.text ?? '')?.group(0);
    if (gregorian != null) return (gregorian, time);
    final cellText = (timeEl.parent?.text ?? '').replaceAll(' ', ' ');
    final idx = cellText.indexOf(time);
    final before = (idx >= 0 ? cellText.substring(0, idx) : cellText)
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    return (before.isEmpty ? null : before, time);
  }

  /// The message body as **cleaned HTML** — keeps links, images, and embedded
  /// media (iframes / `<video>`, e.g. Telegram/YouTube) so the reader can render
  /// them inline. Only ads, the title line, and the "in reply to N" line are
  /// stripped. Returns null when there's no real text *and* no media.
  String? _bodyHtml(dom.Element table) {
    final cell = table.querySelector('tr[bgcolor="#FDFDFD"] td') ??
        table.querySelector('tr[bgcolor="#fdfdfd"] td');
    if (cell == null) return null;
    final clone = cell.clone(true);

    // Telegram embeds arrive as a widget <script data-telegram-post="chan/123">
    // (which can't run in the renderer). Swap each for an embeddable iframe so
    // the post shows inline.
    for (final s in clone.querySelectorAll('script[data-telegram-post]')) {
      final post = s.attributes['data-telegram-post'];
      if (post == null || post.isEmpty) continue;
      final iframe = dom.Element.tag('iframe')
        ..attributes['src'] = 'https://t.me/$post?embed=1'
        ..attributes['width'] = '100%'
        ..attributes['height'] = '480';
      s.replaceWith(iframe);
    }

    // Drop ads + the title line + the "in reply to message N" line.
    for (final junk in clone.querySelectorAll(
        'script, style, ins, noscript, .text16b, a[href^="#"], '
        'div[id*="gpt-ad"], div[id*="taboola"], '
        'iframe[src*="doubleclick"], iframe[src*="googlesyndication"]')) {
      junk.remove();
    }
    // Neutralize ALL of rotter's legacy presentational attributes + inline
    // styles (tiny blue Arial, layout widths/heights/margins, the inline-table
    // wrapper that rendered as a big empty gap) so the body flows in the app's
    // own typography. Structure, links and media stay.
    for (final el in clone.querySelectorAll('*')) {
      for (final attr in const [
        'color', 'face', 'size', 'style', 'align', 'valign', 'width', 'height',
        'bgcolor', 'cellpadding', 'cellspacing', 'border', 'nowrap', 'class'
      ]) {
        el.attributes.remove(attr);
      }
    }

    final hasMedia = clone.querySelector('img, iframe, video, embed, source') != null;
    final text = (clone.text).replaceAll(' ', ' ').trim();
    if (text.isEmpty && !hasMedia) return null;

    // Rotter wraps post text in layout <table>s and pads it with stray <br> runs
    // and empty paragraphs — both render as big vertical gaps. Unwrap the tables
    // (let the text flow) and collapse the breaks so it reads tight.
    var out = clone.innerHtml
        .replaceAll(RegExp(r'<!--.*?-->', dotAll: true), '') // HTML comments
        .replaceAll(RegExp(r'&nbsp;'), ' ')
        // unwrap layout table scaffolding
        .replaceAll(
            RegExp(r'</?(table|tbody|thead|tr|td|th)[^>]*>', caseSensitive: false), ' ')
        // paragraphs → a single line break (uniform, tight spacing)
        .replaceAll(RegExp(r'</?p[^>]*>', caseSensitive: false), '<br>')
        // rotter wraps whole bodies in <b>; the body text should read normal
        // weight (the title is a separate bold widget). NB: \bb> avoids <br>.
        .replaceAll(RegExp(r'</?b>', caseSensitive: false), '')
        // empty inline/block wrappers (only whitespace / <br> inside) → gone
        .replaceAll(
            RegExp(r'<(div|center|font|span|b|i|u)[^>]*>(?:\s|<br\s*/?>)*</\1>',
                caseSensitive: false),
            '')
        // 2+ consecutive line breaks → a single break
        .replaceAll(RegExp(r'(?:\s*<br\s*/?>\s*){2,}', caseSensitive: false), '<br>');

    // Strip leading / trailing <br> even when they sit inside (or between) inline
    // wrappers like <font>/<b>, where they'd otherwise add an empty first/last line.
    final lead = RegExp(
        r'^((?:\s|<(?!br)[a-zA-Z][^>]*>|</[a-zA-Z][^>]*>)*)<br\s*/?>\s*',
        caseSensitive: false);
    while (lead.hasMatch(out)) {
      out = out.replaceFirstMapped(lead, (m) => m.group(1)!);
    }
    final trail = RegExp(r'<br\s*/?>\s*((?:\s|</[a-zA-Z][^>]*>)*)$', caseSensitive: false);
    while (trail.hasMatch(out)) {
      out = out.replaceFirstMapped(trail, (m) => m.group(1)!);
    }
    return out.trim();
  }

  String _decodeEntities(String s) => html_parser.parseFragment(s).text ?? s;

  DateTime? _parseRfc822(String? s) {
    if (s == null || s.isEmpty) return null;
    // e.g. "Fri, 19 Jun 2026 17:22:46 +0300"
    final m = RegExp(
            r'(\d{1,2})\s+(\w{3})\s+(\d{4})\s+(\d{2}):(\d{2}):(\d{2})\s*([+-]\d{4})?')
        .firstMatch(s);
    if (m == null) return null;
    const months = {
      'Jan': 1, 'Feb': 2, 'Mar': 3, 'Apr': 4, 'May': 5, 'Jun': 6,
      'Jul': 7, 'Aug': 8, 'Sep': 9, 'Oct': 10, 'Nov': 11, 'Dec': 12,
    };
    final month = months[m.group(2)];
    if (month == null) return null;
    final dt = DateTime.utc(
      int.parse(m.group(3)!),
      month,
      int.parse(m.group(1)!),
      int.parse(m.group(4)!),
      int.parse(m.group(5)!),
      int.parse(m.group(6)!),
    );
    // Apply the offset to get true UTC, then localize for display.
    final off = m.group(7);
    if (off != null) {
      final sign = off[0] == '-' ? -1 : 1;
      final h = int.parse(off.substring(1, 3));
      final min = int.parse(off.substring(3, 5));
      return dt.subtract(Duration(hours: sign * h, minutes: sign * min)).toLocal();
    }
    return dt.toLocal();
  }
}

/// A non-200 from rotter. A 404 means the thread doesn't exist — removed by a
/// moderator, or so new rotter hasn't generated its static page yet.
class RotterHttpException implements Exception {
  final int status;
  final String url;
  const RotterHttpException(this.status, this.url);
  bool get isNotFound => status == 404;
  @override
  String toString() => 'HTTP $status for $url';
}

/// Member stats read as `<n> <label>`. NEGATIVE points are written with the
/// Hebrew word "מינוס" before the number (e.g. "מינוס 3 נקודות" = -3), not a
/// minus sign — AND rotter closes a <b> between the number and the label
/// ("מינוס 27</b> נקודות"), so tags are allowed between them.
int? parseStat(String label, String html) {
  final m = RegExp('(מינוס\\s+)?([\\d,]+)\\s*(?:</?[^>]+>\\s*)*$label').firstMatch(html);
  if (m == null) return null;
  final n = int.tryParse(m.group(2)!.replaceAll(',', ''));
  if (n == null) return null;
  return m.group(1) != null ? -n : n;
}

/// Per-card metadata the RSS doesn't carry.
typedef CardMeta = ({String? author, int? authorPoints, int replies, DateTime? lastComment});

final _redClockThenDate = RegExp(
    r'''color=["']?red["']?[^>]*>\s*(\d{1,2}:\d{2})\s*</font>[\s\S]{0,300}?(\d{1,2}\.\d{1,2}\.\d{2,4})''',
    caseSensitive: false);

/// Pulls the handful of facts a list card needs straight out of the raw HTML,
/// without building a DOM — opening the app does this for ~75 pages of
/// 50–400KB, and the full parse dominated. `parser_test` pins the output against
/// [RotterService.parseThread] on a real captured thread.
CardMeta parseCardMeta(String html) {
  // The original post's slice: from its <a name="0"> anchor to the next
  // message's anchor, so the FIRST COMMENTER's points aren't read as the poster's.
  var root = '';
  final anchor = RegExp(r'<a\s+name="0"[^>]*>', caseSensitive: false).firstMatch(html);
  if (anchor != null) {
    final rest = html.substring(anchor.end);
    final next = RegExp(r'<a\s+name="\d', caseSensitive: false).firstMatch(rest);
    root = next == null ? rest : rest.substring(0, next.start);
  }
  final author = RegExp(r'^\s*(?:<b>)?\s*([^<]+)').firstMatch(root)?.group(1)?.trim();

  final nums = RegExp(r'<a\s+name="(\d+)"', caseSensitive: false)
      .allMatches(html)
      .map((m) => m.group(1))
      .toSet();

  // Every header is `…<font color=red>HH:MM</font> … DD.MM.YY…`: pair each red
  // clock with the gregorian date that follows it. The window is bounded so a
  // header missing its date can't pair with the next message's.
  DateTime? last;
  for (final m in _redClockThenDate.allMatches(html)) {
    final ts = RotterService.commentTimestamp(m.group(2), m.group(1));
    if (ts != null && (last == null || ts.isAfter(last))) last = ts;
  }

  return (
    author: author == null || author.isEmpty ? null : author,
    authorPoints: parseStat('נקודות', root),
    replies: nums.contains('0') ? nums.length - 1 : nums.length,
    lastComment: last,
  );
}
