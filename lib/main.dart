import 'package:flutter/material.dart';

import 'l10n/app_localizations.dart';
import 'screens/home_shell.dart';
import 'services/auth_service.dart';
import 'services/my_replies_store.dart';
import 'services/read_store.dart';
import 'services/rotter_login.dart';
import 'services/settings_controller.dart';
import 'theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SettingsController.instance.load();
  await AuthService.instance.load();
  await ReadStore.instance.load();
  await MyRepliesStore.instance.load();
  // Refresh the rotter session in the background from saved credentials so the
  // user stays signed in across launches (no blocking, no re-login prompt).
  RotterLogin.refreshSession();
  runApp(const RotterApp());
}

class RotterApp extends StatelessWidget {
  const RotterApp({super.key});
  @override
  Widget build(BuildContext context) {
    final s = SettingsController.instance;
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: s.mode,
      builder: (context, mode, _) => ValueListenableBuilder<Accent>(
        valueListenable: s.accent,
        builder: (context, accent, _) => ValueListenableBuilder<Locale?>(
          valueListenable: s.locale,
          builder: (context, locale, _) => MaterialApp(
            onGenerateTitle: (context) => L10n.of(context)!.appTitle,
            debugShowCheckedModeBanner: false,
            theme: buildTheme(accent, Brightness.light),
            darkTheme: buildTheme(accent, Brightness.dark),
            themeMode: mode,
            locale: locale,
            localizationsDelegates: L10n.localizationsDelegates,
            supportedLocales: L10n.supportedLocales,
            // Chrome follows the locale and reads naturally; the Hebrew *content*
            // is wrapped RTL at the screen level. The text-size setting is applied
            // on top of the device's own scaling.
            builder: (context, child) => ValueListenableBuilder<double>(
              valueListenable: s.textScale,
              builder: (context, scale, _) => MediaQuery(
                data: MediaQuery.of(context).copyWith(
                  textScaler: TextScaler.linear(
                      scale * MediaQuery.textScalerOf(context).scale(1.0)),
                ),
                child: child!,
              ),
            ),
            home: const HomeShell(),
          ),
        ),
      ),
    );
  }
}
