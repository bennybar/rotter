import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../nav.dart';
import '../services/auth_service.dart';
import '../theme.dart';
import 'compose_screen.dart';
import 'login_screen.dart';

/// The "new message" tab. Composing a new scoop thread requires a signed-in
/// account, so this either prompts to sign in or opens the native composer.
class NewMessageScreen extends StatelessWidget {
  const NewMessageScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l.tabNewMessage)),
      body: SafeArea(
        top: false,
        child: ValueListenableBuilder<bool>(
          valueListenable: AuthService.instance.loggedIn,
          builder: (context, loggedIn, _) =>
              loggedIn ? _Composer(l: l) : _SignInPrompt(l: l),
        ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  final L10n l;
  const _Composer({required this.l});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.edit_note_rounded, size: 64, color: accent),
            const SizedBox(height: 18),
            Text(l.compose,
                style: Theme.of(context).textTheme.headlineSmall, textAlign: TextAlign.center),
            const SizedBox(height: 28),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
                ),
                onPressed: () async {
                  final posted = await Navigator.of(context)
                      .push<bool>(modernRoute(const ComposeScreen()));
                  if (posted == true && context.mounted) {
                    ScaffoldMessenger.of(context)
                        .showSnackBar(SnackBar(content: Text(l.postSuccess)));
                  }
                },
                icon: const Icon(Icons.add_rounded),
                label: Text(l.compose),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SignInPrompt extends StatelessWidget {
  final L10n l;
  const _SignInPrompt({required this.l});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.lock_outline_rounded, size: 60, color: accent),
            const SizedBox(height: 20),
            Text(l.newMessagePrompt,
                style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            Text(l.newMessagePromptBody,
                style: TextStyle(color: cMuted(context), height: 1.5),
                textAlign: TextAlign.center),
            const SizedBox(height: 28),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
                ),
                onPressed: () => openLogin(context),
                icon: const Icon(Icons.login_rounded),
                label: Text(l.signIn),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
