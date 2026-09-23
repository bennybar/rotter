import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:rotter_scoops/services/rotter_service.dart';
import 'package:rotter_scoops/services/win1255.dart';

void main() {
  group('win1255', () {
    test('decodes Hebrew letters and shekel sign', () {
      // 0xE0..0xE2 = alef/bet/gimel; 0xA4 = ₪
      expect(decodeWin1255([0xE0, 0xE1, 0xE2]), 'אבג');
      expect(decodeWin1255([0xA4]), '₪');
      expect(decodeWin1255([0x41, 0x42]), 'AB'); // ASCII passthrough
    });

    test('decodes cp1255 percent-encoded usernames', () {
      // %E0=א %F8=ר %E8=ט
      expect(decodeWin1255Percent('%E0%F8%E8'), 'ארט');
      expect(decodeWin1255Percent('a+b'), 'a b');
    });
  });

  group('parseRss', () {
    test('extracts id, title, date from items', () {
      const xml = '''
<?xml version="1.0" encoding="windows-1255"?>
<rss version="0.91"><channel>
  <item>
    <pubDate>Sun, 21 Jun 2026 06:11:36 +0300</pubDate>
    <title>כותרת לדוגמה</title>
    <link>https://rotter.net/forum/scoops1/954033.shtml</link>
  </item>
</channel></rss>''';
      final scoops = RotterService.instance.parseRss(xml);
      expect(scoops.length, 1);
      expect(scoops.first.id, '954033');
      expect(scoops.first.title, 'כותרת לדוגמה');
      final p = scoops.first.published!;
      expect(p.year, 2026);
      expect(p.month, 6);
      expect(p.day, 21);
    });
  });

  group('parseThread', () {
    // Minimal DCForum-shaped HTML covering the parser's selectors.
    const html = '''
<html><body>
<table>
  <tr><td><a name="0"><b>שמעון</b></a></td></tr>
  <tr><td><h1 class="text16b">כותרת ראשית</h1></td></tr>
  <tr><td><font face="Arial" color="#000099"><font color="black">יום ראשון</font>
      <font color="red">06:11</font></font></td></tr>
  <tr bgcolor="#FDFDFD"><td>גוף ההודעה הראשית
    <script async data-telegram-post="N12chat/213825"></script>
    <a href="https://t.me/N12chat/213825">לחץ כאן</a></td></tr>
</table>
<table>
  <tr><td><a name="1"><b>לוי</b></a> <img src="/img/5_star.gif"></td></tr>
  <tr><td><font class="text16b">1. כותרת תגובה</font></td></tr>
  <tr><td><font color="#000099">21.06.26 <font color="red">06:14</font></font></td></tr>
  <tr bgcolor="#FDFDFD"><td><font class="text16b">1. כותרת תגובה</font>
    <a href="#0">בתגובה להודעה מספר 0</a>טקסט התגובה</td></tr>
</table>
</body></html>''';

    final thread = RotterService.instance.parseThread(html, '954033');

    test('builds root + comment tree', () {
      expect(thread.messages.length, 2);
      expect(thread.root!.num, 0);
      expect(thread.root!.author, 'שמעון');
      expect(thread.root!.title, 'כותרת ראשית');
      expect(thread.comments.single.parent, 0);
    });

    test('parses rating, title (number stripped), and time', () {
      final c = thread.comments.single;
      expect(c.author, 'לוי');
      expect(c.rating, 5);
      expect(c.title, 'כותרת תגובה'); // "1. " prefix dropped
      expect(c.time, '06:14');
      expect(c.timestamp, DateTime(2026, 6, 21, 6, 14));
    });

    test('root date is the Hebrew calendar string + red time', () {
      expect(thread.root!.time, '06:11');
      expect(thread.root!.date, contains('יום ראשון'));
    });

    test('telegram script becomes an inline iframe in the body', () {
      final body = thread.root!.bodyHtml!;
      expect(body, contains('<iframe'));
      expect(body, contains('t.me/N12chat/213825?embed=1'));
      expect(body, contains('גוף ההודעה'));
    });

    test('comment body strips the title line and the reply-to line', () {
      final body = thread.comments.single.bodyHtml!;
      expect(body, contains('טקסט התגובה'));
      expect(body, isNot(contains('בתגובה להודעה מספר')));
      expect(body, isNot(contains('כותרת תגובה')));
    });
  });

  // Real pages captured from rotter.net (raw cp1255 bytes), shared with the iOS
  // port. The thread has 59 messages, deep nesting and seven negative-points
  // ("מינוס") members.
  group('captured thread 960077', () {
    final html =
        decodeWin1255(File('test/fixtures/thread-960077.html').readAsBytesSync());
    final thread = RotterService.instance.parseThread(html, '960077');

    test('every comment gets a gregorian timestamp', () {
      expect(thread.comments.length, 58);
      expect(thread.comments.where((m) => m.timestamp == null), isEmpty);
      expect(thread.comments.first.date, matches(RegExp(r'^\d{1,2}\.\d{1,2}\.\d{2}$')));
    });

    test('negative points are parsed', () {
      expect(thread.messages.where((m) => (m.points ?? 0) < 0).length, 7);
    });

    test('card-meta scan agrees with the full parse', () {
      final meta = parseCardMeta(html);
      final latest = thread.comments
          .map((m) => m.timestamp!)
          .reduce((a, b) => a.isAfter(b) ? a : b);
      expect(meta.author, thread.root!.author);
      expect(meta.authorPoints, thread.root!.points);
      expect(meta.replies, thread.comments.length);
      expect(meta.lastComment, latest);
    });
  });

  test('captured feed parses all 74 items', () {
    final xml = decodeWin1255(File('test/fixtures/rotternews.xml').readAsBytesSync());
    expect(RotterService.instance.parseRss(xml).length, 74);
  });
}
