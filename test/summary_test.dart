import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rotter_scoops/screens/summary_screen.dart';

void main() {
  test('splits headings, bullets, numbered items and paragraphs', () {
    final b = summaryBlocks('## ההודעה\nשורה אחת\nוהמשך\n\n- נקודה **חשובה**\n2. שני');
    expect(b.map((x) => x.kind).toList(), [
      SummaryBlockKind.heading,
      SummaryBlockKind.paragraph,
      SummaryBlockKind.item,
      SummaryBlockKind.item,
    ]);
    expect(b[1].text, 'שורה אחת והמשך');
    expect(b[3].marker, '2.');
  });

  test('direction: Hebrew opening with a Latin brand stays RTL; English stays LTR', () {
    expect(textDirectionOf('OpenAI הודיעה על מודל חדש היום'), TextDirection.rtl);
    expect(textDirectionOf('The Israeli site רוטר broke the story'), TextDirection.ltr);
  });
}
