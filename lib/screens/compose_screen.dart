import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../services/auth_service.dart';
import '../services/my_replies_store.dart';
import '../services/rotter_post.dart';
import '../theme.dart';
import 'login_screen.dart' show openLogin;

/// Native composer for a reply, a new thread, or editing your own message.
/// Submits directly to rotter (no webview UI). On failure it toasts an error —
/// there is no website fallback. Pops `true` when posted/saved.
class ComposeScreen extends StatefulWidget {
  /// Reply target; `null` (with no [editNum]) means this composes a NEW thread.
  final String? threadId;
  final int parentNum; // 0 = reply to the original post

  /// When set, EDIT this message of [threadId] instead of posting (0 = the root).
  final int? editNum;

  const ComposeScreen({super.key, this.threadId, this.parentNum = 0, this.editNum});

  bool get isEdit => editNum != null;
  bool get isNewThread => threadId == null && !isEdit;

  @override
  State<ComposeScreen> createState() => _ComposeScreenState();
}

class _ComposeScreenState extends State<ComposeScreen> {
  final _subject = TextEditingController();
  final _body = TextEditingController();
  bool _busy = false;
  bool _loadingDraft = false;

  @override
  void initState() {
    super.initState();
    if (widget.isEdit) _loadDraft();
  }

  @override
  void dispose() {
    _subject.dispose();
    _body.dispose();
    super.dispose();
  }

  /// Pull the message's current text off rotter's edit form to pre-fill.
  Future<void> _loadDraft() async {
    setState(() => _loadingDraft = true);
    final draft = await RotterPost.loadForEdit(
        threadId: widget.threadId!, num: widget.editNum!);
    if (!mounted) return;
    if (draft != null) {
      _subject.text = draft.subject;
      _body.text = draft.body;
    }
    setState(() => _loadingDraft = false);
    if (draft == null && mounted) {
      final l = L10n.of(context)!;
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.postFailed), duration: const Duration(seconds: 3)));
    }
  }

  Future<void> _send() async {
    final body = _body.text.trim();
    final subject = _subject.text.trim();
    // A title alone is enough — content isn't required if there's a title.
    if (subject.isEmpty && body.isEmpty) return;
    FocusScope.of(context).unfocus();

    if (!AuthService.instance.loggedIn.value) {
      final ok = await openLogin(context);
      if (!ok || !mounted) return;
    }

    setState(() => _busy = true);
    Future<PostOutcome> submit() {
      if (widget.isEdit) {
        return RotterPost.edit(
            threadId: widget.threadId!,
            num: widget.editNum!,
            subject: subject,
            body: body);
      }
      if (widget.isNewThread) return RotterPost.newThread(subject: subject, body: body);
      return RotterPost.reply(
          threadId: widget.threadId!,
          parentNum: widget.parentNum,
          subject: subject,
          body: body);
    }

    var outcome = await submit();
    if (outcome == PostOutcome.notLoggedIn && mounted) {
      final ok = await openLogin(context);
      if (ok) outcome = await submit();
    }
    if (!mounted) return;

    if (outcome == PostOutcome.success) {
      HapticFeedback.mediumImpact();
      // Editing an existing message isn't a new reply of ours to remember.
      if (!widget.isNewThread && !widget.isEdit) {
        await MyRepliesStore.instance.add(widget.threadId!);
      }
      if (!mounted) return;
      Navigator.of(context).pop(true);
      return;
    }

    // Couldn't post — toast the error and let them retry (no website fallback).
    final l = L10n.of(context)!;
    ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l.postFailed), duration: const Duration(seconds: 3)));
    setState(() => _busy = false);
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    final title = widget.isEdit
        ? l.edit
        : (widget.isNewThread ? l.compose : l.reply);
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          Padding(
            padding: const EdgeInsetsDirectional.only(end: 8),
            child: TextButton(
              onPressed: (_busy || _loadingDraft) ? null : _send,
              child: _busy
                  ? const SizedBox(
                      width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2.2))
                  : Text(widget.isEdit ? l.save : l.send,
                      style: TextStyle(
                          fontWeight: FontWeight.w800, fontSize: 15.5, color: accent)),
            ),
          ),
        ],
      ),
      body: _loadingDraft
          ? const Center(child: CircularProgressIndicator())
          : Directionality(
        textDirection: TextDirection.rtl,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Both replies and new threads carry a title on rotter (optional on
              // replies — empty keeps rotter's "Re:…" default).
              TextField(
                controller: _subject,
                // Focus the title on load, but not when editing (text is prefilled).
                autofocus: !widget.isEdit,
                enabled: !_busy,
                textInputAction: TextInputAction.next,
                style: TextStyle(
                    fontSize: 18, fontWeight: FontWeight.w800, color: cInk(context)),
                decoration: InputDecoration(
                  hintText: l.subjectHint,
                  border: InputBorder.none,
                ),
              ),
              Divider(color: cField(context), height: 1),
              const SizedBox(height: 4),
              Expanded(
                child: TextField(
                  controller: _body,
                  autofocus: false,
                  enabled: !_busy,
                  maxLines: null,
                  expands: true,
                  textAlignVertical: TextAlignVertical.top,
                  keyboardType: TextInputType.multiline,
                  style: TextStyle(fontSize: 16, height: 1.5, color: cInk(context)),
                  decoration: InputDecoration(
                    hintText: widget.isNewThread ? l.bodyHint : l.composeHint,
                    border: InputBorder.none,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
