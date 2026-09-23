import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:rotter_scoops/services/read_store.dart';
import 'package:rotter_scoops/services/rotter_service.dart';
import 'package:rotter_scoops/services/scoop_meta.dart';
import 'package:shared_preferences/shared_preferences.dart';

CardMeta meta(int replies) =>
    (author: 'a', authorPoints: 1, replies: replies, lastComment: null);

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('a visible card jumps ahead of the prefetched feed', () async {
    final order = <String>[];
    final gate = Completer<void>();
    final cache = ScoopMetaCache(
      persists: false,
      fetch: (id) async {
        order.add(id);
        await gate.future;
        return meta(1);
      },
    );
    // 20 fill every slot; 21..30 wait in the queue.
    cache.prefetch([for (var i = 0; i < 30; i++) '$i']);
    cache.ensure('29');
    gate.complete();
    await pumpEventQueue();
    expect(order.indexOf('29'), 20); // first one started after the initial 20
    expect(cache.progress.value, isNull); // batch finished and reset
  });

  test('a failed refresh keeps the value already shown', () async {
    var fail = false;
    final cache = ScoopMetaCache(
      persists: false,
      fetch: (id) async => fail ? throw Exception('offline') : meta(5),
    );
    cache.ensure('x');
    await pumpEventQueue();
    expect(cache.of('x')?.replies, 5);
    fail = true;
    cache.revalidate();
    cache.ensure('x');
    await pumpEventQueue();
    expect(cache.of('x')?.replies, 5);
    expect(cache.isPending('x'), isFalse);
  });

  test('404 is unavailable and not retried', () async {
    var calls = 0;
    final cache = ScoopMetaCache(
      persists: false,
      fetch: (id) async {
        calls++;
        throw const RotterHttpException(404, 'u');
      },
    );
    cache.ensure('gone');
    await pumpEventQueue();
    cache.ensure('gone');
    await pumpEventQueue();
    expect(cache.isUnavailable('gone'), isTrue);
    expect(calls, 1);
  });

  test('arrivals reconcile read state (new comments on a read thread)', () async {
    await ReadStore.instance.load();
    await ReadStore.instance.markRead('t', 3);
    final cache = ScoopMetaCache(persists: false, fetch: (id) async => meta(4));
    cache.ensure('t');
    await pumpEventQueue();
    expect(ReadStore.instance.isNew('t'), isTrue);
    expect(ReadStore.instance.isRead('t'), isFalse);
  });
}
