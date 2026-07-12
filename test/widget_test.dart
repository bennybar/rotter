// Smoke test: the app builds and shows the bottom navigation.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:rotter_scoops/main.dart';
import 'package:rotter_scoops/services/auth_service.dart';
import 'package:rotter_scoops/services/read_store.dart';
import 'package:rotter_scoops/services/settings_controller.dart';

void main() {
  testWidgets('app builds with bottom navigation', (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});
    await SettingsController.instance.load();
    await AuthService.instance.load();
    await ReadStore.instance.load();

    // Let the (mocked-400) scoops fetch settle to its error state so the loading
    // skeleton's repeating shimmer animation isn't left running at teardown.
    await tester.runAsync(() async {
      await tester.pumpWidget(const RotterApp());
      await Future<void>.delayed(const Duration(milliseconds: 150));
    });
    await tester.pump();

    expect(find.byType(NavigationBar), findsOneWidget);
  });
}
