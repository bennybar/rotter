package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.data.MapKeyValue
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.ReadStore
import com.bennybarak.scoops.rotter_scoops.data.ScoopMetaCache
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.CardMeta
import com.bennybarak.scoops.rotter_scoops.net.RotterHttpException
import com.bennybarak.scoops.rotter_scoops.net.parseCardMeta
import com.bennybarak.scoops.rotter_scoops.ui.screens.ScoopsController
import com.bennybarak.scoops.rotter_scoops.ui.screens.decodeCachedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewFixesTest {
    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Prefs.backend = MapKeyValue()
        ReadStore.load()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    // 1. A cache that is valid JSON but not valid scoops is a miss, not a crash.
    @Test fun invalidCachedListIsIgnored() {
        assertNull(decodeCachedList(JSONArray("""[{"title":"missing id and url"}]""")))
        assertNull(decodeCachedList(JSONArray("""[{"id":5,"title":"t","url":{}}]""")))
        assertNull(decodeCachedList(JSONArray("""["not an object"]""")))
        val ok = decodeCachedList(JSONArray("""[{"id":"1","title":"t","url":"u","published":5}]"""))
        assertEquals("1", ok!!.single().id)
    }

    // 2. A disposed list controller no longer listens to the app-wide sort setting.
    @Test fun disposedControllerUnregistersSortListener() = runTest {
        val before = SettingsController.sortListenerCount
        val controllers = (1..3).map { ScoopsController(backgroundScope) }
        assertEquals(before + 3, SettingsController.sortListenerCount)
        controllers.forEach { it.dispose() }
        controllers.first().dispose() // twice is harmless
        assertEquals(before, SettingsController.sortListenerCount)
    }

    // 3. A thread that 404s (not generated yet) is retried after the cooldown.
    @Test fun notFoundRecoversAfterCooldown() = runTest(UnconfinedTestDispatcher()) {
        var clock = 0L
        var calls = 0
        val cache = ScoopMetaCache(
            fetch = {
                calls++
                if (calls == 1) throw RotterHttpException(404, "u") else CardMeta("a", 1, 2, null)
            },
            persists = false,
            scope = this,
            now = { clock },
        )
        cache.ensure("fresh")
        advanceUntilIdle()
        assertTrue(cache.isUnavailable("fresh"))
        cache.ensure("fresh") // inside the cooldown: no request
        advanceUntilIdle()
        assertEquals(1, calls)
        clock += ScoopMetaCache.NOT_FOUND_RETRY_MS + 1
        cache.ensure("fresh")
        advanceUntilIdle()
        assertEquals(2, calls)
        assertFalse(cache.isUnavailable("fresh"))
        assertEquals(2, cache.of("fresh")?.replies)
    }

    @Test fun pullToRefreshRetriesNotFound() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val cache = ScoopMetaCache(
            fetch = { calls++; if (calls == 1) throw RotterHttpException(404, "u") else CardMeta("a", 1, 2, null) },
            persists = false,
            scope = this,
            now = { 0L },
        )
        cache.ensure("x")
        advanceUntilIdle()
        cache.revalidate()
        cache.ensure("x")
        advanceUntilIdle()
        assertEquals(2, calls)
        assertFalse(cache.isUnavailable("x"))
    }

    // A card that came back without its poster is looked at again soon.
    @Test fun authorlessMetaIsRefetchedSoon() = runTest(UnconfinedTestDispatcher()) {
        var clock = 0L
        var calls = 0
        val cache = ScoopMetaCache(
            fetch = { calls++; CardMeta(if (calls == 1) null else "a", null, 1, null) },
            persists = false,
            scope = this,
            now = { clock },
        )
        cache.ensure("n")
        advanceUntilIdle()
        clock += 31_000
        cache.ensure("n")
        advanceUntilIdle()
        assertEquals("a", cache.of("n")?.author)
    }

    @Test fun cardMetaAuthorWithUnquotedAnchorAndWrappers() {
        val html = """<a name=0><font color="#000"><b>חיים_הגנן</b></font></a> 5 נקודות
            <a name=1><b>לוי</b></a><a name=2><b>x</b></a>"""
        val m = parseCardMeta(html)
        assertEquals("חיים_הגנן", m.author)
        assertEquals(2, m.replies)
    }
}
