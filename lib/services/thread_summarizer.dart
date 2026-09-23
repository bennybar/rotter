import 'dart:convert';

import 'package:html/parser.dart' as html_parser;
import 'package:http/http.dart' as http;

import '../models/message.dart';

class SummaryException implements Exception {
  final String message;
  const SummaryException(this.message);
  @override
  String toString() => message;
}

/// Summarizes a thread (original post + comments) with an OpenAI-compatible
/// chat-completions model. Entirely opt-in: it only runs when the user turned it
/// on and supplied their own key, because it sends the thread's text to a
/// third party.
class ThreadSummarizer {
  final String apiKey;
  final String model;
  final String baseUrl;

  /// English name of the answer language for the prompt ("Hebrew"/"English").
  final String language;

  const ThreadSummarizer({
    required this.apiKey,
    required this.model,
    required this.baseUrl,
    required this.language,
  });

  /// Threads run to hundreds of comments; the transcript is capped and the
  /// OLDEST comments kept — they carry the substance, later ones tend to argue.
  static const _maxTranscriptChars = 12000;

  Future<String> summarize(Thread thread, String title) async {
    final root = baseUrl.trim().replaceFirst(RegExp(r'/+$'), '');
    final uri = Uri.tryParse('$root/chat/completions');
    if (uri == null) throw const SummaryException('Invalid endpoint');

    final res = await http
        .post(
          uri,
          headers: {
            'Authorization': 'Bearer $apiKey',
            'Content-Type': 'application/json',
          },
          body: jsonEncode({
            'model': model,
            // No `temperature`: this model rejects it.
            'messages': [
              {'role': 'system', 'content': _systemPrompt(language)},
              {'role': 'user', 'content': _transcript(thread, title)},
            ],
          }),
        )
        .timeout(const Duration(seconds: 60));

    Map<String, dynamic>? json;
    try {
      json = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    } catch (_) {}
    if (res.statusCode != 200) {
      // A bad key or unknown model comes back as a JSON error body — its message
      // beats a bare status, especially since the model name is user-editable.
      final msg = (json?['error'] as Map?)?['message'] as String?;
      throw SummaryException(msg ?? 'HTTP ${res.statusCode}');
    }
    final text = (((json?['choices'] as List?)?.firstOrNull as Map?)?['message']
            as Map?)?['content'] as String?;
    if (text == null || text.trim().isEmpty) throw const SummaryException('');
    return text.trim();
  }

  static String _systemPrompt(String language) => '''
You summarize discussion threads from rotter.net, an Israeli news forum. You will be given the original post followed by its comments.

Write the summary in $language.

Format it as Markdown with exactly these three sections, each introduced by a `## ` heading translated into $language:

## The post
Two or three sentences on what the original post claims.

## The responses
Bullet points, one per position taken in the thread — grouped by argument rather than listed comment by comment. Note where commenters disagree with the post or with each other.

## What's new
Bullet points for anything presented as new information, each saying whether the thread corroborates it.

Use `-` for bullets and `**bold**` for the few phrases that matter most. Do not use tables, code blocks or headings other than the three above. Keep the whole summary under 250 words.

Do not invent details that are not in the text. rotter posts are often unverified rumor — describe claims as claims, and say so plainly when the thread itself disputes them.''';

  static String _transcript(Thread thread, String title) {
    final lines = ['POST: $title'];
    final root = thread.root;
    if (root != null) {
      lines.add('BY: ${root.author}');
      if (root.bodyHtml != null) lines.add(_plain(root.bodyHtml!));
    }
    lines.add('\nCOMMENTS:');
    var budget = _maxTranscriptChars;
    for (final c in thread.comments) {
      final entry =
          '- ${c.author}: ${c.title ?? ''} ${c.bodyHtml == null ? '' : _plain(c.bodyHtml!)}'.trim();
      if (entry.length >= budget) break;
      budget -= entry.length;
      lines.add(entry);
    }
    return lines.join('\n');
  }

  static String _plain(String html) =>
      (html_parser.parseFragment(html).text ?? '').replaceAll(RegExp(r'\s+'), ' ').trim();
}
