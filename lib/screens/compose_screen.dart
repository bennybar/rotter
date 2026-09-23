import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../nav.dart';
import '../services/auth_service.dart';
import '../services/draft_store.dart';
import '../services/my_replies_store.dart';
import '../services/rotter_post.dart';
import '../theme.dart';
import 'login_screen.dart' show openLogin;

/// Open [screen], signing in first when needed. Reply controls stay enabled
/// while signed out — an inert button reads as broken rather than gated — so
/// this signs in and then opens the composer that was actually asked for.
/// Returns the composer's result (true = posted), or null if sign-in was
/// abandoned.
Future<bool?> openComposer(BuildContext context, ComposeScreen screen) async {
  if (!AuthService.instance.loggedIn.value) {
    final ok = await openLogin(context);
    if (!ok || !context.mounted) return null;
  }
  return Navigator.of(context).push<bool>(modernRoute(screen));
}

/// Native composer for a reply, a new thread, or editing your own message.
/// Submits directly to rotter (no webview UI). On failure it toasts an error —
/// there is no website fallback. Pops `true` when posted/saved.
class ComposeScreen extends StatefulWidget {
  /// Reply target; `null` (with no [editNum]) means this composes a NEW thread.
  final String? threadId;
  final int parentNum; // 0 = reply to the original post

  /// When set, EDIT this message of [threadId] instead of posting (0 = the root).
  final int? editNum;

  /// True when this is a tab's root (the New message tab) rather than a pushed
  /// screen: nothing to pop, so a successful post clears the fields instead.
  final bool embedded;

  const ComposeScreen(
      {super.key, this.threadId, this.parentNum = 0, this.editNum, this.embedded = false});

  bool get isEdit => editNum != null;
  bool get isNewThread => threadId == null && !isEdit;

  /// Which draft this composer reads and writes.
  String get draftKey => isEdit
      ? 'e:$threadId:$editNum'
      : isNewThread
          ? 'new'
          : 'r:$threadId:$parentNum';

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
    if (widget.isEdit) {
      _loadDraft();
    } else {
      _restoreDraft();
    }
    // Autosaved as it's typed, so a back-swipe or a failed send never costs the
    // user their text.
    _subject.addListener(_saveDraft);
    _body.addListener(_saveDraft);
  }

  void _saveDraft() {
    if (_loadingDraft) return;
    DraftStore.instance.save(widget.draftKey, _subject.text, _body.text);
  }

  void _restoreDraft() {
    final d = DraftStore.instance.draft(widget.draftKey);
    if (d == null) return;
    _subject.text = d.subject;
    _body.text = d.body;
  }

  bool get _hasText => _subject.text.trim().isNotEmpty || _body.text.trim().isNotEmpty;

  /// The explicit close offers to throw the text away; a back-swipe keeps it
  /// (it's autosaved).
  Future<void> _close() async {
    if (!_hasText) {
      Navigator.of(context).pop(false);
      return;
    }
    final l = L10n.of(context)!;
    final discard = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: Text(l.discardDraftTitle),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l.keepEditing)),
          TextButton(
            style: TextButton.styleFrom(foregroundColor: Colors.red.shade400),
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l.discard),
          ),
        ],
      ),
    );
    if (discard != true || !mounted) return;
    await DraftStore.instance.clear(widget.draftKey);
    if (mounted) Navigator.of(context).pop(false);
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
    // A saved draft is newer than whatever the server had, so it wins.
    _restoreDraft();
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
      await DraftStore.instance.clear(widget.draftKey);
      if (!mounted) return;
      if (widget.embedded) {
        // A tab root has nowhere to go: clear the fields and stay put.
        _subject.clear();
        _body.clear();
        setState(() => _busy = false);
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(L10n.of(context)!.postSuccess)));
      } else {
        Navigator.of(context).pop(true);
      }
      return;
    }

    // Couldn't post — toast the error and let them retry (no website fallback);
    // the text stays, and is in the draft too.
    final l = L10n.of(context)!;
    final msg = switch (outcome) {
      PostOutcome.notLoggedIn => l.notSignedInError,
      PostOutcome.blocked => l.blockedError,
      _ => l.postFailed,
    };
    ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(msg), duration: const Duration(seconds: 3)));
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
        automaticallyImplyLeading: !widget.embedded,
        leading: widget.embedded
            ? null
            : IconButton(
                tooltip: l.cancel,
                icon: const Icon(Icons.close_rounded),
                onPressed: _busy ? null : _close,
              ),
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
          : SafeArea(
        top: false,
        child: Directionality(
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
                // Focus the title on load, but not when editing (text is prefilled)
                // or as a tab root (IndexedStack builds it at launch — the keyboard
                // would pop over the scoops list).
                autofocus: !widget.isEdit && !widget.embedded,
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
      ),
    );
  }
}
