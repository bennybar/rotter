import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../theme.dart';

/// A small, balanced palette so every author gets a stable identity colour for
/// their avatar (works on both the light/cream and dark backgrounds).
const List<Color> _avatarPalette = [
  Color(0xFF3B82F6), // blue
  Color(0xFF10B981), // emerald
  Color(0xFFEC4899), // pink
  Color(0xFF8B5CF6), // violet
  Color(0xFFF59E0B), // amber
  Color(0xFF06B6D4), // cyan
  Color(0xFFEF6C4D), // coral
  Color(0xFF64748B), // slate
];

/// Hashed by content with FNV-1a + a splitmix64 finaliser. `% 8` keeps only the
/// low three bits, which barely move between similar inputs under a simple
/// hash — over a real thread six of fourteen rotter names collided on one
/// colour — and Hebrew names share leading UTF-8 bytes. The finaliser
/// avalanches the whole hash into those bits.
Color avatarColor(String name) {
  var h = 0xcbf29ce484222325;
  for (final b in name.codeUnits) {
    h = (h ^ b) * 0x100000001b3;
  }
  h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9;
  h = (h ^ (h >>> 27)) * 0x94d049bb133111eb;
  h ^= h >>> 31;
  return _avatarPalette[h % _avatarPalette.length];
}

/// A round, per-author identity bubble showing the first letter.
Widget avatarBubble(String name, double size) {
  final c = avatarColor(name);
  final t = name.trim();
  return Container(
    width: size,
    height: size,
    alignment: Alignment.center,
    decoration: BoxDecoration(color: c.withValues(alpha: 0.16), shape: BoxShape.circle),
    child: Text(t.isEmpty ? '?' : t.substring(0, 1),
        style: TextStyle(color: c, fontWeight: FontWeight.w800, fontSize: size * 0.5)),
  );
}

/// Reputation-points pill (red when negative). Tapping opens the member profile.
Widget pointsChip(BuildContext context, int pts, VoidCallback? onTap) {
  final c = pts < 0 ? Colors.red.shade400 : cMuted(context);
  return Tooltip(
    message: L10n.of(context)!.memberPoints,
    child: GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
        decoration:
            BoxDecoration(color: c.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(999)),
        // LTR so a negative renders as "-3", not "3-" in the RTL thread.
        child: Text('$pts',
            textDirection: TextDirection.ltr,
            style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: c)),
      ),
    ),
  );
}
