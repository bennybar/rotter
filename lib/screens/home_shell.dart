import 'dart:io' show Platform;
import 'dart:ui' show ImageFilter;

import 'package:cupertino_native/cupertino_native.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'new_message_screen.dart';
import 'scoops_screen.dart';
import 'settings_screen.dart';

/// Root section navigation.
/// - iOS: a native `CNTabBar` (real UIKit → system Liquid Glass on iOS 26),
///   translucent by default with content scrolling behind it.
/// - Android: a Material 3 `NavigationBar` over a blurred "glass" surface.
///
/// `extendBody` lets the lists run full-screen behind the bar.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  void _select(int i) {
    if (i == _index) return;
    HapticFeedback.selectionClick();
    setState(() => _index = i);
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;

    // (materialOff, materialOn, sfSymbol, label) — search is in the Scoops app bar.
    final items = <(IconData, IconData, String, String)>[
      (Icons.bolt_outlined, Icons.bolt_rounded, 'bolt.fill', l.tabScoops),
      (Icons.edit_outlined, Icons.edit_rounded, 'square.and.pencil', l.tabNewMessage),
      (Icons.settings_outlined, Icons.settings_rounded, 'gearshape.fill', l.tabSettings),
    ];
    const screens = <Widget>[
      ScoopsScreen(),
      NewMessageScreen(),
      SettingsScreen(),
    ];
    final index = _index.clamp(0, screens.length - 1);

    return Scaffold(
      extendBody: true, // content scrolls behind the translucent tab bar
      body: IndexedStack(index: index, children: screens),
      bottomNavigationBar: Platform.isIOS
          ? CNTabBar(
              currentIndex: index,
              onTap: _select,
              tint: accent,
              items: [
                for (final t in items) CNTabBarItem(label: t.$4, icon: CNSymbol(t.$3)),
              ],
            )
          : _GlassNavBar(
              index: index,
              onSelect: _select,
              accent: accent,
              items: items,
            ),
    );
  }
}

/// Android: a translucent, blurred NavigationBar so the list shows through it.
class _GlassNavBar extends StatelessWidget {
  final int index;
  final ValueChanged<int> onSelect;
  final Color accent;
  final List<(IconData, IconData, String, String)> items;

  const _GlassNavBar({
    required this.index,
    required this.onSelect,
    required this.accent,
    required this.items,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRect(
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 22, sigmaY: 22),
        child: NavigationBar(
          selectedIndex: index,
          onDestinationSelected: onSelect,
          backgroundColor: cSurface(context).withValues(alpha: 0.72),
          elevation: 0,
          surfaceTintColor: Colors.transparent,
          shadowColor: Colors.transparent,
          destinations: [
            for (final t in items)
              NavigationDestination(
                icon: Icon(t.$1),
                selectedIcon: Icon(t.$2, color: accent),
                label: t.$4,
              ),
          ],
        ),
      ),
    );
  }
}
