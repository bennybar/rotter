package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.data.MapKeyValue
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.ReadStore
import com.bennybarak.scoops.rotter_scoops.data.ScoopMetaCache
import com.bennybarak.scoops.rotter_scoops.net.CardMeta
import com.bennybarak.scoops.rotter_scoops.net.RotterHttpException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScoopMetaTest {
    private fun meta(replies: Int) = CardMeta("a", 1, replies, null)

    @Before fun setUp() {
        Prefs.backend = MapKeyValue()
        ReadStore.load()
    }

    @Test fun visibleCardJumpsAheadOfPrefetchedFeed() = runTest(UnconfinedTestDispatcher()) {
        val order = ArrayList<String>()
        val gate = CompletableDeferred<Unit>()
        val cache = ScoopMetaCache(
            fetch = { id -> order.add(id); gate.await(); meta(1) },
            persists = false,
            scope = this,
        )
        // 20 fill every slot; 21..30 wait in the queue.
        cache.prefetch((0 until 30).map { "$it" })
        cache.ensure("29")
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(20, order.indexOf("29")) // first one started after the initial 20
        assertNull(cache.progress) // batch finished and reset
    }

    @Test fun failedRefreshKeepsShownValue() = runTest(UnconfinedTestDispatcher()) {
        var fail = false
        val cache = ScoopMetaCache(
            fetch = { if (fail) throw Exception("offline") else meta(5) },
            persists = false,
            scope = this,
        )
        cache.ensure("x")
        advanceUntilIdle()
        assertEquals(5, cache.of("x")?.replies)
        fail = true
        cache.revalidate()
        cache.ensure("x")
        advanceUntilIdle()
        assertEquals(5, cache.of("x")?.replies)
        assertFalse(cache.isPending("x"))
    }

    @Test fun notFoundIsUnavailableAndNotRetried() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val cache = ScoopMetaCache(
            fetch = { calls++; throw RotterHttpException(404, "u") },
            persists = false,
            scope = this,
        )
        cache.ensure("gone")
        advanceUntilIdle()
        cache.ensure("gone")
        advanceUntilIdle()
        assertTrue(cache.isUnavailable("gone"))
        assertEquals(1, calls)
    }

    @Test fun arrivalsReconcileReadState() = runTest(UnconfinedTestDispatcher()) {
        ReadStore.markRead("t", 3)
        val cache = ScoopMetaCache(fetch = { meta(4) }, persists = false, scope = this)
        cache.ensure("t")
        advanceUntilIdle()
        assertTrue(ReadStore.isNew("t"))
        assertFalse(ReadStore.isRead("t"))
    }

    @Test fun readStorePersistsInFlutterFormat() {
        ReadStore.markRead("123", 7)
        // Same key + JSON shape the Flutter build wrote, so updates keep read state.
        assertEquals("{\"123\":7}", Prefs.getString("read_threads"))
        Prefs.setDouble("text_scale", 1.2)
        assertEquals(1.2, Prefs.getDouble("text_scale")!!, 0.0)
    }
}
