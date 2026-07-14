import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../theme.dart';

/// How the scoops list is ordered. `lastComment` is rotter's own default (most
/// recently active first); `postTime` orders strictly by when each was posted.
enum SortMode { lastComment, postTime }

/// App-wide preferences (theme mode, accent, language), persisted across launches.
///
/// Language default is **Hebrew** regardless of device locale — `null` here means
/// "follow device", and the stored default on first launch is Hebrew.
class SettingsController {
  SettingsController._();
  static final SettingsController instance = SettingsController._();

  static const _modeKey = 'theme_mode';
  static const _accentKey = 'accent';
  static const _localeKey = 'locale'; // 'system' | 'he' | 'en'
  static const _scaleKey = 'text_scale';
  static const _sortKey = 'sort_mode'; // 'lastComment' | 'postTime'
  static const _densityKey = 'thread_density';
  static const _predictiveBackKey = 'predictive_back';

  /// Readable text-size range, applied on top of the device's own scaling.
  static const double minScale = 0.9;
  static const double maxScale = 1.5;

  /// Multiplier on the in-thread comment padding/gaps (lower = tighter).
  static const double minDensity = 0.4;
  static const double maxDensity = 1.2;

  final ValueNotifier<ThemeMode> mode = ValueNotifier(ThemeMode.system);
  final ValueNotifier<Accent> accent = ValueNotifier(Accent.amber);

  /// `null` → follow device locale; otherwise a forced locale.
  final ValueNotifier<Locale?> locale = ValueNotifier(const Locale('he'));

  /// In-app text size factor (1.0 = default, comfortable for Hebrew reading).
  final ValueNotifier<double> textScale = ValueNotifier(1.0);

  /// Scoops list ordering; defaults to rotter's own "last comment" order.
  final ValueNotifier<SortMode> sortMode = ValueNotifier(SortMode.lastComment);

  /// In-thread comment spacing factor (1.0 = roomy; default is tighter).
  final ValueNotifier<double> threadDensity = ValueNotifier(0.7);

  /// Android back gesture: true = system predictive back (drag peeks the previous
  /// screen); false = the classic fade + slide-up transition. No effect on iOS,
  /// which always uses the Cupertino interactive edge-swipe.
  final ValueNotifier<bool> predictiveBack = ValueNotifier(true);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();

    mode.value = switch (prefs.getString(_modeKey)) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };

    accent.value = Accent.values.firstWhere(
      (a) => a.name == prefs.getString(_accentKey),
      orElse: () => Accent.amber,
    );

    // Default to Hebrew on first launch (no stored value).
    locale.value = switch (prefs.getString(_localeKey)) {
      'system' => null,
      'en' => const Locale('en'),
      _ => const Locale('he'),
    };

    final s = prefs.getDouble(_scaleKey);
    if (s != null) textScale.value = s.clamp(minScale, maxScale);

    sortMode.value =
        prefs.getString(_sortKey) == 'postTime' ? SortMode.postTime : SortMode.lastComment;

    final d = prefs.getDouble(_densityKey);
    if (d != null) threadDensity.value = d.clamp(minDensity, maxDensity);

    predictiveBack.value = prefs.getBool(_predictiveBackKey) ?? true;
  }

  Future<void> setPredictiveBack(bool v) async {
    predictiveBack.value = v;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_predictiveBackKey, v);
  }

  Future<void> setThreadDensity(double v) async {
    final clamped = double.parse(v.clamp(minDensity, maxDensity).toStringAsFixed(2));
    threadDensity.value = clamped;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_densityKey, clamped);
  }

  Future<void> setSortMode(SortMode m) async {
    sortMode.value = m;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_sortKey, m.name);
  }

  Future<void> setMode(ThemeMode m) async {
    mode.value = m;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_modeKey, m.name);
  }

  Future<void> setAccent(Accent a) async {
    accent.value = a;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_accentKey, a.name);
  }

  Future<void> setLocale(Locale? l) async {
    locale.value = l;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_localeKey, l?.languageCode ?? 'system');
  }

  Future<void> setTextScale(double v) async {
    final clamped = double.parse(v.clamp(minScale, maxScale).toStringAsFixed(2));
    textScale.value = clamped;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_scaleKey, clamped);
  }
}
