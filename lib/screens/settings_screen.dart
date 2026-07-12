import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../services/auth_service.dart';
import '../services/settings_controller.dart';
import '../theme.dart';
import 'login_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final s = SettingsController.instance;
    return Scaffold(
      appBar: AppBar(title: Text(l.settingsTitle)),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: EdgeInsets.fromLTRB(18, 8, 18, 28 + MediaQuery.paddingOf(context).bottom),
          children: [
            _label(context, l.appearance),
            const SizedBox(height: 10),
            _card(
              context,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _rowLabel(context, Icons.dark_mode_rounded, l.theme),
                    const SizedBox(height: 12),
                    ValueListenableBuilder<ThemeMode>(
                      valueListenable: s.mode,
                      builder: (context, mode, _) => _ThemeSelector(mode: mode),
                    ),
                    const SizedBox(height: 22),
                    _rowLabel(context, Icons.palette_rounded, l.accentColor),
                    const SizedBox(height: 14),
                    ValueListenableBuilder<Accent>(
                      valueListenable: s.accent,
                      builder: (context, accent, _) => _AccentPicker(selected: accent),
                    ),
                    const SizedBox(height: 22),
                    _rowLabel(context, Icons.format_size_rounded, l.textSize),
                    const SizedBox(height: 4),
                    const _TextSizeSlider(),
                    const SizedBox(height: 18),
                    _rowLabel(context, Icons.density_medium_rounded, l.threadSpacing),
                    const SizedBox(height: 4),
                    const _ThreadDensitySlider(),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            _card(
              context,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _rowLabel(context, Icons.translate_rounded, l.language),
                    const SizedBox(height: 12),
                    ValueListenableBuilder<Locale?>(
                      valueListenable: s.locale,
                      builder: (context, locale, _) => _LanguageSelector(locale: locale),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            _card(
              context,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _rowLabel(context, Icons.sort_rounded, l.sortBy),
                    const SizedBox(height: 12),
                    ValueListenableBuilder<SortMode>(
                      valueListenable: s.sortMode,
                      builder: (context, sort, _) => _SortSelector(mode: sort),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 28),
            _label(context, l.account),
            const SizedBox(height: 10),
            _card(
              context,
              child: ValueListenableBuilder<bool>(
                valueListenable: AuthService.instance.loggedIn,
                builder: (context, loggedIn, _) => loggedIn
                    ? ListTile(
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                        leading: Icon(Icons.logout_rounded,
                            color: Theme.of(context).colorScheme.primary),
                        title: Text(l.signOut,
                            style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
                        subtitle: ValueListenableBuilder<String?>(
                          valueListenable: AuthService.instance.username,
                          builder: (context, user, _) => Text(user ?? l.signedIn,
                              style: TextStyle(color: cMuted(context))),
                        ),
                        onTap: () => AuthService.instance.signOut(),
                      )
                    : ListTile(
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                        leading: Icon(Icons.login_rounded,
                            color: Theme.of(context).colorScheme.primary),
                        title: Text(l.signIn,
                            style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
                        subtitle: Text(l.loginSubtitle, style: TextStyle(color: cMuted(context))),
                        onTap: () => openLogin(context),
                      ),
              ),
            ),
            const SizedBox(height: 28),
            _label(context, l.about),
            const SizedBox(height: 10),
            _card(
              context,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(l.aboutBody,
                    style: TextStyle(color: cMuted(context), height: 1.5)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _label(BuildContext context, String text) => Padding(
        padding: const EdgeInsetsDirectional.only(start: 4),
        child: Text(text,
            style: TextStyle(
                fontSize: 12, fontWeight: FontWeight.w800, letterSpacing: 1, color: cMuted(context))),
      );

  Widget _rowLabel(BuildContext context, IconData icon, String text) => Row(children: [
        Icon(icon, size: 20, color: cInk(context)),
        const SizedBox(width: 10),
        Text(text,
            style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w700, color: cInk(context))),
      ]);

  Widget _card(BuildContext context, {required Widget child}) => Container(
        decoration: BoxDecoration(
          color: cSurface(context),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
                color: Colors.black.withValues(alpha: 0.04),
                blurRadius: 14,
                offset: const Offset(0, 5)),
          ],
        ),
        child: child,
      );
}

class _ThemeSelector extends StatelessWidget {
  final ThemeMode mode;
  const _ThemeSelector({required this.mode});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final opts = [
      (ThemeMode.light, Icons.light_mode_rounded, l.themeLight),
      (ThemeMode.dark, Icons.dark_mode_rounded, l.themeDark),
      (ThemeMode.system, Icons.phone_iphone_rounded, l.themeSystem),
    ];
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(color: cField(context), borderRadius: BorderRadius.circular(14)),
      child: Row(
        children: [
          for (final o in opts)
            Expanded(
              child: _segment(context, o.$2, o.$3,
                  selected: mode == o.$1, onTap: () => SettingsController.instance.setMode(o.$1)),
            ),
        ],
      ),
    );
  }
}

class _SortSelector extends StatelessWidget {
  final SortMode mode;
  const _SortSelector({required this.mode});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final opts = <(SortMode, IconData, String)>[
      (SortMode.lastComment, Icons.forum_rounded, l.sortLastComment),
      (SortMode.postTime, Icons.schedule_rounded, l.sortPostTime),
    ];
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(color: cField(context), borderRadius: BorderRadius.circular(14)),
      child: Row(
        children: [
          for (final o in opts)
            Expanded(
              child: _segment(context, o.$2, o.$3,
                  selected: mode == o.$1,
                  onTap: () => SettingsController.instance.setSortMode(o.$1)),
            ),
        ],
      ),
    );
  }
}

class _LanguageSelector extends StatelessWidget {
  final Locale? locale;
  const _LanguageSelector({required this.locale});

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final opts = <(Locale?, IconData, String)>[
      (const Locale('he'), Icons.abc_rounded, l.hebrew),
      (const Locale('en'), Icons.abc_rounded, l.english),
      (null, Icons.phone_iphone_rounded, l.languageSystem),
    ];
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(color: cField(context), borderRadius: BorderRadius.circular(14)),
      child: Row(
        children: [
          for (final o in opts)
            Expanded(
              child: _segment(context, o.$2, o.$3,
                  selected: locale?.languageCode == o.$1?.languageCode,
                  onTap: () => SettingsController.instance.setLocale(o.$1)),
            ),
        ],
      ),
    );
  }
}

Widget _segment(BuildContext context, IconData icon, String label,
    {required bool selected, required VoidCallback onTap}) {
  final accent = Theme.of(context).colorScheme.primary;
  return GestureDetector(
    behavior: HitTestBehavior.opaque,
    onTap: () {
      if (selected) return;
      HapticFeedback.selectionClick();
      onTap();
    },
    child: AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      padding: const EdgeInsets.symmetric(vertical: 11),
      decoration: BoxDecoration(
        color: selected ? cSurface(context) : Colors.transparent,
        borderRadius: BorderRadius.circular(11),
        boxShadow: selected
            ? [BoxShadow(color: Colors.black.withValues(alpha: 0.08), blurRadius: 8, offset: const Offset(0, 2))]
            : null,
      ),
      child: Column(
        children: [
          Icon(icon, size: 21, color: selected ? accent : cMuted(context)),
          const SizedBox(height: 5),
          Text(label,
              style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: selected ? cInk(context) : cMuted(context))),
        ],
      ),
    ),
  );
}

class _TextSizeSlider extends StatelessWidget {
  const _TextSizeSlider();

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    final s = SettingsController.instance;
    return ValueListenableBuilder<double>(
      valueListenable: s.textScale,
      builder: (context, scale, _) => Row(
        children: [
          Text('א', style: TextStyle(fontSize: 14, color: cMuted(context))),
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: accent,
                thumbColor: accent,
                overlayColor: accent.withValues(alpha: 0.14),
              ),
              child: Slider(
                value: scale,
                min: SettingsController.minScale,
                max: SettingsController.maxScale,
                divisions: 6,
                label: '${(scale * 100).round()}%',
                onChanged: (v) {
                  HapticFeedback.selectionClick();
                  s.setTextScale(v);
                },
              ),
            ),
          ),
          Text('א', style: TextStyle(fontSize: 24, color: cMuted(context))),
        ],
      ),
    );
  }
}

class _ThreadDensitySlider extends StatelessWidget {
  const _ThreadDensitySlider();

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    final s = SettingsController.instance;
    return ValueListenableBuilder<double>(
      valueListenable: s.threadDensity,
      builder: (context, density, _) => Row(
        children: [
          Icon(Icons.density_small_rounded, size: 16, color: cMuted(context)),
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: accent,
                thumbColor: accent,
                overlayColor: accent.withValues(alpha: 0.14),
              ),
              child: Slider(
                value: density,
                min: SettingsController.minDensity,
                max: SettingsController.maxDensity,
                divisions: 8,
                label: '${(density * 100).round()}%',
                onChanged: (v) {
                  HapticFeedback.selectionClick();
                  s.setThreadDensity(v);
                },
              ),
            ),
          ),
          Icon(Icons.density_large_rounded, size: 22, color: cMuted(context)),
        ],
      ),
    );
  }
}

class _AccentPicker extends StatelessWidget {
  final Accent selected;
  const _AccentPicker({required this.selected});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 14,
      runSpacing: 14,
      children: [
        for (final a in Accent.values)
          GestureDetector(
            onTap: () {
              HapticFeedback.selectionClick();
              SettingsController.instance.setAccent(a);
            },
            child: Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: a.seed,
                shape: BoxShape.circle,
                border: Border.all(
                  color: a == selected ? cInk(context) : Colors.transparent,
                  width: 3,
                ),
                boxShadow: [
                  BoxShadow(color: a.seed.withValues(alpha: 0.4), blurRadius: 8, offset: const Offset(0, 3)),
                ],
              ),
              child: a == selected
                  ? const Icon(Icons.check_rounded, color: Colors.white, size: 22)
                  : null,
            ),
          ),
      ],
    );
  }
}
