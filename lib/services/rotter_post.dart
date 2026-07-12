import 'package:flutter/foundation.dart';
import 'package:html/parser.dart' as html_parser;

import 'auth_service.dart';
import 'rotter_gated.dart';
import 'win1255.dart';

enum PostOutcome { success, notLoggedIn, blocked, error }

/// The current text of a message being edited, read off rotter's edit form.
class EditDraft {
  final String subject;
  final String body;
  const EditDraft({required this.subject, required this.body});
}

/// Posts / edits on rotter EXACTLY the way the Android app does
/// (`NetworkManager.sendCommentSubject`): GET the relevant form only to read its
/// hidden `rand` token, then POST a body built with explicit field names:
///
///   `az=a_mesg&rand=…&name=…&om=…&omm=…&forum=scoops1&subject=…&body=…`
///
/// `az=a_mesg` posts (reply + new thread), `az=e_mesg` edits. A new thread drops
/// name/om/omm and adds `topic_type=0`; editing the root post also sends
/// `topic_type`. All windows-1255 urlencoded, through [RotterGated] (the webview
/// fetch, so it carries the Cloudflare + session cookies). Success = the 302
/// redirect back to the thread (`GatedResponse.redirected`).
class RotterPost {
  RotterPost._();

  static const _base = 'https://rotter.net/cgi-bin/forum/dcboard.cgi';

  static String _editUrl(String threadId, int num) =>
      '$_base?az=edit&forum=scoops1&om=$threadId&omm=$num';

  /// Reply to [threadId] under message [parentNum] (0 = reply to the original post).
  static Future<PostOutcome> reply({
    required String threadId,
    required int parentNum,
    String subject = '',
    required String body,
  }) =>
      _post(
        az: 'a_mesg',
        getUrl: '$_base?az=post&forum=scoops1&om=$threadId&omm=$parentNum',
        om: threadId,
        omm: parentNum,
        subject: subject,
        body: body,
      );

  /// Start a new scoop thread with [subject] + [body].
  static Future<PostOutcome> newThread({
    required String subject,
    required String body,
  }) =>
      _post(
        az: 'a_mesg',
        getUrl: '$_base?az=post&forum=scoops1',
        subject: subject,
        body: body,
        topicType: true,
      );

  /// Edit the user's own message [num] in [threadId] (0 = the root post).
  static Future<PostOutcome> edit({
    required String threadId,
    required int num,
    required String subject,
    required String body,
  }) =>
      _post(
        az: 'e_mesg',
        getUrl: _editUrl(threadId, num),
        om: threadId,
        omm: num,
        subject: subject,
        body: body,
        topicType: num == 0, // the APK sends topic_type when editing the root
      );

  /// Read the current subject/body of message [num] from rotter's edit form, to
  /// pre-fill the composer. Null when it can't be loaded (not signed in, not the
  /// author, network).
  static Future<EditDraft?> loadForEdit({
    required String threadId,
    required int num,
  }) async {
    final r = await RotterGated.request(_editUrl(threadId, num));
    if (r == null || r.status == 403) return null;
    final doc = html_parser.parse(r.text);
    final textarea = doc.querySelector('textarea');
    if (textarea == null) {
      debugPrint('RotterPost.loadForEdit: no textarea (not signed in / not author?)');
      return null;
    }
    final subject = doc.querySelector('input[name="subject"]')?.attributes['value'] ??
        doc.querySelector('input[type="text"]')?.attributes['value'] ??
        '';
    return EditDraft(subject: subject, body: textarea.text);
  }

  static Future<PostOutcome> _post({
    required String az,
    required String getUrl,
    String? om, // thread id; null for a new thread (no name/om/omm)
    int? omm, // reply parent, or the message being edited
    required String subject,
    required String body,
    bool topicType = false,
  }) async {
    // 1. GET the form, only to read its hidden `rand` token.
    final r1 = await RotterGated.request(getUrl);
    if (r1 == null) return PostOutcome.error;
    final formHtml = r1.text;
    if (r1.status == 403 || formHtml.toLowerCase().contains('just a moment')) {
      return PostOutcome.blocked;
    }

    final doc = html_parser.parse(formHtml);
    // No textarea → we're not signed in (or may not edit this message).
    if (doc.querySelector('textarea') == null) return PostOutcome.notLoggedIn;
    final rand = doc.querySelector('input[name="rand"]')?.attributes['value'] ??
        doc.querySelector('input[name="random"]')?.attributes['value'];
    if (rand == null) {
      debugPrint('RotterPost: no rand token in form (len=${formHtml.length})');
      return PostOutcome.error;
    }

    // 2. Build the POST body with the APK's exact field names.
    final user = AuthService.instance.username.value ?? '';
    final fields = <String, String>{
      'az': az,
      'rand': rand,
      if (om != null) ...{'name': user, 'om': om, 'omm': '$omm'},
      'forum': 'scoops1',
      'subject': subject,
      'body': body,
      if (topicType) 'topic_type': '0',
    };

    final r2 = await RotterGated.request(_base, method: 'POST', body: encodeWin1255Form(fields));
    if (r2 == null) return PostOutcome.error;
    if (r2.status == 403) return PostOutcome.blocked;
    debugPrint('RotterPost $az: status=${r2.status} redirected=${r2.redirected} '
        'len=${r2.text.length}');
    // The APK treats a 302 redirect (to the thread) as success; via fetch that
    // surfaces as `redirected`.
    return r2.redirected ? PostOutcome.success : PostOutcome.error;
  }
}
