import 'package:flutter/material.dart';

/// Bundled Hebrew-first typeface (assets/fonts) — no runtime download.
const String kFontFamily = 'NotoSansHebrew';

/// Selectable accent colors. The first is the default (amber).
enum Accent { amber, red, blue, green, purple, graphite }

extension AccentColor on Accent {
  Color get seed => switch (this) {
        Accent.amber => const Color(0xFFF57C00),
        Accent.red => const Color(0xFFD32030),
        Accent.blue => const Color(0xFF1565C0),
        Accent.green => const Color(0xFF2E7D32),
        Accent.purple => const Color(0xFF6A3DE8),
        Accent.graphite => const Color(0xFF4A5160),
      };
}

// Neutral ink/background ramps, shared across accents.
const Color _kInk = Color(0xFF15130E);
const Color _kMuted = Color(0xFF8A8378);
const Color _kBg = Color(0xFFF6F4F0);

const Color _kInkDark = Color(0xFFF0ECE4);
const Color _kMutedDark = Color(0xFFA39C90);
const Color _kBgDark = Color(0xFF141310);
const Color _kSurfaceDark = Color(0xFF1E1C18);
const Color _kFieldDark = Color(0xFF28251F);
const Color _kField = Color(0xFFECE9E3);

bool _isDark(BuildContext c) => Theme.of(c).brightness == Brightness.dark;

Color cInk(BuildContext c) => _isDark(c) ? _kInkDark : _kInk;
Color cMuted(BuildContext c) => _isDark(c) ? _kMutedDark : _kMuted;
Color cBg(BuildContext c) => _isDark(c) ? _kBgDark : _kBg;
Color cSurface(BuildContext c) => _isDark(c) ? _kSurfaceDark : Colors.white;
Color cField(BuildContext c) => _isDark(c) ? _kFieldDark : _kField;

ThemeData buildTheme(Accent accent, Brightness brightness) {
  final dark = brightness == Brightness.dark;
  final scheme = ColorScheme.fromSeed(
    seedColor: accent.seed,
    primary: accent.seed,
    brightness: brightness,
  ).copyWith(
    surface: dark ? _kSurfaceDark : Colors.white,
    onSurface: dark ? _kInkDark : _kInk,
    onSurfaceVariant: dark ? _kMutedDark : _kMuted,
  );
  final ink = dark ? _kInkDark : _kInk;
  final bg = dark ? _kBgDark : _kBg;
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    brightness: brightness,
    fontFamily: kFontFamily,
  );
  final text = base.textTheme;

  return base.copyWith(
    scaffoldBackgroundColor: bg,
    splashFactory: InkSparkle.splashFactory,
    // Back-gesture "sneak peek": on Android, predictive back shrinks the current
    // page and reveals the previous one behind it as you drag (needs
    // android:enableOnBackInvokedCallback=true in the manifest, and Android 13+
    // for the animation — older versions just fall back to the normal transition).
    // iOS keeps the Cupertino interactive edge-swipe, which already parallaxes the
    // previous page in underneath.
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: {
        TargetPlatform.android: PredictiveBackPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      },
    ),
    textTheme: text.copyWith(
      headlineMedium: text.headlineMedium
          ?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.5, color: ink),
      headlineSmall: text.headlineSmall
          ?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.3, color: ink),
      titleLarge: text.titleLarge
          ?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.2, color: ink),
      titleMedium: text.titleMedium?.copyWith(fontWeight: FontWeight.w700, color: ink),
      bodyMedium: text.bodyMedium
          ?.copyWith(color: dark ? const Color(0xFFCFCABF) : const Color(0xFF3C382F)),
    ),
    appBarTheme: AppBarTheme(
      centerTitle: false,
      scrolledUnderElevation: 0.5,
      backgroundColor: bg,
      surfaceTintColor: Colors.transparent,
      foregroundColor: ink,
      titleTextStyle: TextStyle(
          fontFamily: kFontFamily,
          color: ink,
          fontSize: 21,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.3),
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: dark ? _kSurfaceDark : Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      clipBehavior: Clip.antiAlias,
    ),
    snackBarTheme: const SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      insetPadding: EdgeInsets.all(14),
    ),
  );
}
