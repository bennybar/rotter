import 'dart:io' show Platform;

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'services/settings_controller.dart';

/// Page transition for pushed screens, with a back-gesture "sneak peek":
///
/// - **iOS**: [CupertinoPageRoute] — the interactive edge-swipe drags the current
///   page away while parallaxing the previous one in underneath. Always on.
/// - **Android**: chosen by the `predictiveBack` setting.
///   - ON → a plain [MaterialPageRoute], so the route honours the theme's
///     `pageTransitionsTheme` (`PredictiveBackPageTransitionsBuilder`) and you get
///     the system peek: the page shrinks and the previous screen shows behind it.
///     NB: a route that defines its own `transitionsBuilder` BYPASSES the theme and
///     silently kills predictive back — which is exactly what the classic route does.
///   - OFF → the classic fade + slide-up [PageRouteBuilder] (no peek).
///
/// On iOS we also wrap the page in [_EdgeBackGestures] so back-swipe works from
/// **both** screen edges (Cupertino's own gesture is leading-edge only, which in
/// an RTL app sits on the right — this adds the left edge too, on every pushed
/// window including the WebViews).
Route<T> modernRoute<T>(Widget page) {
  if (Platform.isIOS) {
    return CupertinoPageRoute<T>(builder: (_) => _EdgeBackGestures(child: page));
  }
  if (SettingsController.instance.predictiveBack.value) {
    return MaterialPageRoute<T>(builder: (_) => page);
  }
  return PageRouteBuilder<T>(
    transitionDuration: const Duration(milliseconds: 320),
    reverseTransitionDuration: const Duration(milliseconds: 240),
    pageBuilder: (_, __, ___) => page,
    transitionsBuilder: (_, animation, __, child) {
      final curved = CurvedAnimation(parent: animation, curve: Curves.easeOutCubic);
      return FadeTransition(
        opacity: curved,
        child: SlideTransition(
          position: Tween(begin: const Offset(0, 0.045), end: Offset.zero).animate(curved),
          child: child,
        ),
      );
    },
  );
}

/// Adds inward-swipe-to-go-back detectors on both screen edges (iOS).
class _EdgeBackGestures extends StatelessWidget {
  final Widget child;
  const _EdgeBackGestures({required this.child});

  static const double _edgeWidth = 24;
  static const double _threshold = 45;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        child,
        _edge(context, start: true),
        _edge(context, start: false),
      ],
    );
  }

  Widget _edge(BuildContext context, {required bool start}) {
    return PositionedDirectional(
      start: start ? 0 : null,
      end: start ? null : 0,
      top: 0,
      bottom: 0,
      width: _edgeWidth,
      child: _EdgeDragToPop(threshold: _threshold),
    );
  }
}

class _EdgeDragToPop extends StatefulWidget {
  final double threshold;
  const _EdgeDragToPop({required this.threshold});

  @override
  State<_EdgeDragToPop> createState() => _EdgeDragToPopState();
}

class _EdgeDragToPopState extends State<_EdgeDragToPop> {
  double _dx = 0;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onHorizontalDragStart: (_) => _dx = 0,
      onHorizontalDragUpdate: (d) => _dx += d.delta.dx,
      onHorizontalDragEnd: (d) {
        final swiped = _dx.abs() > widget.threshold ||
            d.primaryVelocity != null && d.primaryVelocity!.abs() > 250;
        if (swiped) Navigator.of(context).maybePop();
      },
    );
  }
}
