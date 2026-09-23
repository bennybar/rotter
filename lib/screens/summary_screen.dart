import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:share_plus/share_plus.dart';

import '../l10n/app_localizations.dart';
import '../models/message.dart';
import '../services/ai_store.dart';
import '../services/settings_controller.dart';
import '../services/thread_summarizer.dart';
import '../theme.dart';

/// An LLM summary of a thread. Only reachable when summaries are enabled and a
/// key is stored ([AIStore.isReady]).
class SummaryScreen extends StatefulWidget {
  final Thread thread;
  final String title;
  const SummaryScreen({super.key, required this.thread, required this.title});

  @override
  State<SummaryScreen> createState() => _SummaryScreenState();
}

class _SummaryScreenState extends State<SummaryScreen> {
  String? _summary;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final ai = AIStore.instance;
    final key = await ai.apiKey();
    if (!mounted) return;
    if (key == null) {
      setState(() {
        _loading = false;
        _error = L10n.of(context)!.aiErrorNotConfigured;
      });
      return;
    }
    try {
      final text = await ThreadSummarizer(
        apiKey: key,
        model: ai.model,
        baseUrl: ai.baseUrl,
        language: ai.promptLanguage(SettingsController.instance.locale.value),
      ).summarize(widget.thread, widget.title);
      if (!mounted) return;
      setState(() {
        _summary = text;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      final msg = e.toString();
      setState(() {
        _loading = false;
        _error = msg.isEmpty ? L10n.of(context)!.aiErrorEmpty : msg;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l.aiSummary),
        actions: [
          if (_summary != null)
            PopupMenuButton<String>(
              onSelected: (v) {
                switch (v) {
                  case 'copy':
                    Clipboard.setData(ClipboardData(text: _summary!));
                    ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(l.copied), duration: const Duration(seconds: 1)));
                  case 'share':
                    SharePlus.instance.share(ShareParams(text: _summary!));
                  case 'regenerate':
                    // Explicit, and explicitly another billed request.
                    _load();
                }
              },
              itemBuilder: (_) => [
                PopupMenuItem(value: 'copy', child: Text(l.copy)),
                PopupMenuItem(value: 'share', child: Text(l.share)),
                const PopupMenuDivider(),
                PopupMenuItem(value: 'regenerate', child: Text(l.aiRegenerate)),
              ],
            ),
        ],
      ),
      body: _body(l),
    );
  }

  Widget _body(L10n l) {
    if (_loading) {
      return Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 14),
          Text(l.aiSummarizing, style: TextStyle(color: cMuted(context))),
        ]),
      );
    }
    if (_summary == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(Icons.error_outline_rounded, size: 46, color: cMuted(context)),
            const SizedBox(height: 12),
            Text(l.aiErrorTitle,
                style: TextStyle(fontWeight: FontWeight.w800, color: cInk(context))),
            if (_error != null) ...[
              const SizedBox(height: 6),
              Text(_error!, textAlign: TextAlign.center, style: TextStyle(color: cMuted(context))),
            ],
            const SizedBox(height: 14),
            FilledButton.tonal(onPressed: _load, child: Text(l.retry)),
          ]),
        ),
      );
    }
    final ink = cInk(context);
    final body = TextStyle(fontSize: 15.5, height: 1.5, color: ink);
    return SelectionArea(
      child: ListView(
        padding: EdgeInsets.fromLTRB(18, 16, 18, 28 + MediaQuery.paddingOf(context).bottom),
        children: [
          // Which thread this is about — the screen title only says "Summary".
          Directionality(
            textDirection: textDirectionOf(widget.title),
            child: Text(widget.title,
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: cMuted(context))),
          ),
          const SizedBox(height: 10),
          // Rendered as blocks: the model answers in Markdown. Each block takes
          // its own direction, so a Hebrew answer hugs the right and an English
          // one the left without knowing which was asked for.
          for (final b in summaryBlocks(_summary!))
            Padding(
              padding: EdgeInsets.only(top: b.kind == SummaryBlockKind.heading ? 14 : 6),
              child: Directionality(
                textDirection: textDirectionOf(b.text),
                child: switch (b.kind) {
                  SummaryBlockKind.heading => Text.rich(_inline(b.text),
                      style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: ink)),
                  SummaryBlockKind.item => Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      SizedBox(
                          width: 22,
                          child: Text(b.marker!, style: body.copyWith(color: cMuted(context)))),
                      Expanded(child: Text.rich(_inline(b.text), style: body)),
                    ]),
                  SummaryBlockKind.paragraph => Text.rich(_inline(b.text), style: body),
                },
              ),
            ),
          const SizedBox(height: 18),
          // rotter runs on unverified rumour, and a fluent summary of a rumour
          // reads far more authoritative than the thread it came from.
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(Icons.info_outline_rounded, size: 16, color: cMuted(context)),
            const SizedBox(width: 6),
            Expanded(
              child: Text(l.aiDisclaimer, style: TextStyle(fontSize: 12.5, color: cMuted(context))),
            ),
          ]),
        ],
      ),
    );
  }

  /// `**bold**` spans; everything else is plain text.
  TextSpan _inline(String text) {
    final spans = <TextSpan>[];
    final re = RegExp(r'\*\*(.+?)\*\*');
    var last = 0;
    for (final m in re.allMatches(text)) {
      if (m.start > last) spans.add(TextSpan(text: text.substring(last, m.start)));
      spans.add(TextSpan(text: m.group(1), style: const TextStyle(fontWeight: FontWeight.w800)));
      last = m.end;
    }
    if (last < text.length) spans.add(TextSpan(text: text.substring(last)));
    return TextSpan(children: spans);
  }
}

enum SummaryBlockKind { heading, item, paragraph }

class SummaryBlock {
  final SummaryBlockKind kind;
  final String text;
  final String? marker; // "•" or "1." for list items
  const SummaryBlock._(this.kind, this.text, [this.marker]);
}

/// Splits the model's Markdown into headings, list items and paragraphs — the
/// block structure a plain Text would otherwise show as literal `##` and `-`.
List<SummaryBlock> summaryBlocks(String markdown) {
  final out = <SummaryBlock>[];
  final para = <String>[];
  void flush() {
    final joined = para.join(' ').trim();
    para.clear();
    if (joined.isNotEmpty) out.add(SummaryBlock._(SummaryBlockKind.paragraph, joined));
  }

  for (final raw in markdown.split('\n')) {
    final line = raw.trim();
    RegExpMatch? m;
    if (line.isEmpty) {
      flush();
    } else if ((m = RegExp(r'^#{1,6}\s+(.*)$').firstMatch(line)) != null) {
      flush();
      out.add(SummaryBlock._(SummaryBlockKind.heading, m!.group(1)!));
    } else if ((m = RegExp(r'^[-*•]\s+(.*)$').firstMatch(line)) != null) {
      flush();
      out.add(SummaryBlock._(SummaryBlockKind.item, m!.group(1)!, '•'));
    } else if ((m = RegExp(r'^(\d{1,2})[.)]\s+(.*)$').firstMatch(line)) != null) {
      flush();
      out.add(SummaryBlock._(SummaryBlockKind.item, m!.group(2)!, '${m.group(1)}.'));
    } else {
      para.add(line);
    }
  }
  flush();
  return out;
}

/// RTL unless the text is plainly left-to-right: predominantly Latin letters
/// AND opening with one. Majority alone mislabels a Hebrew line quoting a long
/// English name; first-strong alone mislabels a Hebrew line opening with a
/// brand ("OpenAI הודיעה…").
TextDirection textDirectionOf(String text) {
  var rtl = 0, ltr = 0;
  bool? firstRtl;
  for (final r in text.runes) {
    final isRtl = (r >= 0x0590 && r <= 0x08FF) || (r >= 0xFB1D && r <= 0xFEFF);
    final isLtr = !isRtl &&
        ((r >= 0x41 && r <= 0x5A) || (r >= 0x61 && r <= 0x7A) || (r >= 0xC0 && r <= 0x24F));
    if (!isRtl && !isLtr) continue;
    firstRtl ??= isRtl;
    isRtl ? rtl++ : ltr++;
  }
  if (rtl > ltr || firstRtl == true) return TextDirection.rtl;
  return firstRtl == false ? TextDirection.ltr : TextDirection.rtl;
}
