import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';

import '../l10n/app_localizations.dart';

/// A short, localized relative time ("just now" / "3h ago" / "לפני 3 שע׳").
/// Falls back to an absolute date for anything older than ~6 days.
String relTime(DateTime when, BuildContext context) {
  final l = L10n.of(context)!;
  final now = DateTime.now();
  final diff = now.difference(when);

  if (diff.isNegative || diff.inMinutes < 1) return l.justNow;
  if (diff.inMinutes < 60) return l.minutesAgo(diff.inMinutes);
  if (diff.inHours < 24) return l.hoursAgo(diff.inHours);
  if (diff.inDays <= 6) return l.daysAgo(diff.inDays);

  final locale = Localizations.localeOf(context).toString();
  return DateFormat('d MMM • HH:mm', locale).format(when);
}
