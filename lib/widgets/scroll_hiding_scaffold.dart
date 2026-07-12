import 'package:flutter/material.dart';

/// A Scaffold whose top bar slides away when the user scrolls down and returns
/// when they scroll up — done WITHOUT slivers (a `SliverAppBar`/`CustomScrollView`
/// has historically tripped a null-geometry semantics crash in this app).
///
/// The bar is OVERLAID on top of the body (not in a Column), so hiding it never
/// resizes the list — which would shift content and re-fire scroll notifications
/// in a jumpy feedback loop. The body's scrollable must reserve [barHeight] of
/// top content padding so its first item isn't hidden under the bar.
class ScrollHidingScaffold extends StatefulWidget {
  /// Height the body's scrollable should add as top padding (so content clears
  /// the overlaid bar). Pass the bar as `AppBar(primary: false, …)`.
  static const double barHeight = kToolbarHeight;

  final PreferredSizeWidget bar;
  final Widget body;
  final Widget? floatingActionButton;
  final FloatingActionButtonLocation? floatingActionButtonLocation;

  const ScrollHidingScaffold({
    super.key,
    required this.bar,
    required this.body,
    this.floatingActionButton,
    this.floatingActionButtonLocation,
  });

  @override
  State<ScrollHidingScaffold> createState() => _ScrollHidingScaffoldState();
}

class _ScrollHidingScaffoldState extends State<ScrollHidingScaffold> {
  bool _visible = true;
  double _lastPixels = 0;
  double _accum = 0; // accumulated drag since the last toggle

  bool _onScroll(ScrollNotification n) {
    if (n.metrics.axis != Axis.vertical) return false;
    final px = n.metrics.pixels;

    // Always reveal near the very top, so the bar can never get stuck hidden.
    if (px <= n.metrics.minScrollExtent + 4) {
      if (!_visible) setState(() => _visible = true);
      _lastPixels = px;
      _accum = 0;
      return false;
    }

    if (n is ScrollUpdateNotification) {
      final delta = px - _lastPixels;
      _lastPixels = px;
      if ((delta > 0) != (_accum > 0)) _accum = 0; // direction flipped → reset
      _accum += delta;
      if (_accum > 36 && _visible) {
        setState(() => _visible = false);
        _accum = 0;
      } else if (_accum < -36 && !_visible) {
        setState(() => _visible = true);
        _accum = 0;
      }
    }
    return false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: widget.floatingActionButton,
      floatingActionButtonLocation: widget.floatingActionButtonLocation,
      body: SafeArea(
        bottom: false,
        child: ClipRect(
          child: Stack(
            children: [
              // The list fills the whole area and scrolls UNDER the bar (it pads
              // its own top by barHeight), so the bar overlay never moves it.
              Positioned.fill(
                child: NotificationListener<ScrollNotification>(
                  onNotification: _onScroll,
                  child: widget.body,
                ),
              ),
              AnimatedPositioned(
                duration: const Duration(milliseconds: 200),
                curve: Curves.easeOutCubic,
                top: _visible ? 0 : -ScrollHidingScaffold.barHeight,
                left: 0,
                right: 0,
                height: ScrollHidingScaffold.barHeight,
                child: widget.bar,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
