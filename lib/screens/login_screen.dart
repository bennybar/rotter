import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../nav.dart';
import '../services/auth_service.dart';
import '../services/rotter_login.dart';
import '../theme.dart';

/// Open our native sign-in screen; returns true if signed in.
Future<bool> openLogin(BuildContext context) async {
  final ok = await Navigator.of(context).push<bool>(modernRoute(const LoginScreen()));
  return ok ?? false;
}

/// Our own sign-in screen. It collects the rotter username + password and signs
/// in through a headless WebView (so Cloudflare just works), saving the
/// credentials in the Keychain so future sessions are silent. On failure it
/// shows an inline error — there is no website fallback.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _user = TextEditingController();
  final _pass = TextEditingController();
  bool _busy = false;
  bool _autoTrying = false; // signing in with saved creds before showing the form
  String? _error;

  @override
  void initState() {
    super.initState();
    _maybeAutoLogin();
  }

  @override
  void dispose() {
    _user.dispose();
    _pass.dispose();
    super.dispose();
  }

  Future<void> _maybeAutoLogin() async {
    final creds = await AuthService.instance.credentials();
    if (creds == null || !mounted) return;
    _user.text = creds.user;
    _pass.text = creds.pass;
    setState(() => _autoTrying = true);
    final r = await RotterLogin.attempt(user: creds.user, pass: creds.pass);
    if (!mounted) return;
    if (r.outcome == LoginOutcome.success) {
      Navigator.of(context).pop(true);
    } else {
      // Saved creds didn't work silently — reveal the form (with an error only
      // if they were actually rejected).
      setState(() {
        _autoTrying = false;
        _error = r.outcome == LoginOutcome.wrongCredentials ? L10n.of(context)!.loginFailed : null;
      });
    }
  }

  Future<void> _submit() async {
    final user = _user.text.trim();
    final pass = _pass.text;
    if (user.isEmpty || pass.isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });

    final r = await RotterLogin.attempt(user: user, pass: pass);
    if (!mounted) return;

    switch (r.outcome) {
      case LoginOutcome.success:
        await AuthService.instance.saveCredentials(user, pass);
        if (mounted) Navigator.of(context).pop(true);
      case LoginOutcome.wrongCredentials:
        setState(() {
          _busy = false;
          _error = L10n.of(context)!.loginFailed;
        });
      case LoginOutcome.failed:
        setState(() {
          _busy = false;
          _error = L10n.of(context)!.loginError;
        });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;

    if (_autoTrying) {
      return Scaffold(
        appBar: AppBar(title: Text(l.signIn)),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(title: Text(l.signIn)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(22, 18, 22, 28),
          children: [
            Text(l.loginSubtitle,
                style: TextStyle(color: cMuted(context), height: 1.5, fontSize: 14.5)),
            const SizedBox(height: 22),
            TextField(
              controller: _user,
              textInputAction: TextInputAction.next,
              autocorrect: false,
              enableSuggestions: false,
              decoration: InputDecoration(
                labelText: l.usernameLabel,
                prefixIcon: const Icon(Icons.person_outline_rounded),
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _pass,
              obscureText: true,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => _busy ? null : _submit(),
              decoration: InputDecoration(
                labelText: l.passwordLabel,
                prefixIcon: const Icon(Icons.lock_outline_rounded),
                border: const OutlineInputBorder(),
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Row(children: [
                Icon(Icons.error_outline_rounded, size: 18, color: Colors.red.shade400),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(_error!,
                      style: TextStyle(color: Colors.red.shade400, fontWeight: FontWeight.w600)),
                ),
              ]),
            ],
            const SizedBox(height: 22),
            FilledButton(
              onPressed: _busy ? null : _submit,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(50),
                backgroundColor: accent,
              ),
              child: _busy
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                  : Text(l.signIn,
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
            ),
          ],
        ),
      ),
    );
  }
}
