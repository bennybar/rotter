package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.net.DigestItem
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.bennybarak.scoops.rotter_scoops.ui.screens.DIGEST_STOPS
import com.bennybarak.scoops.rotter_scoops.ui.screens.DigestState
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestTest {
    private val now = 1_800_000_000_000L

    @org.junit.Before fun setUp() {
        com.bennybarak.scoops.rotter_scoops.data.Prefs.backend = com.bennybarak.scoops.rotter_scoops.data.MapKeyValue()
    }

    // Digests are kept for a day: a scoop one of them covered isn't sent again.
    @Test fun keptDigestsCoverTheirScoopsForADay() {
        val prefs = com.bennybarak.scoops.rotter_scoops.data.Prefs
        val h = 60 * 60_000L
        prefs.setString(
            "digests",
            """[{"at":${now - 2 * h},"minutes":60,"ids":["b","c"],"text":"## x"},""" +
                """{"at":${now - 25 * h},"minutes":60,"ids":["old"],"text":"## y"}]""",
        )
        val s = DigestState(TestScope()) { now }
        assertEquals(1, s.kept.size) // the 25-hour-old one is gone
        assertEquals(setOf("b", "c"), s.covered())
        // Everything in the window already digested: no request, "up to date".
        val feed = listOf(scoop("b", 20), scoop("c", 40), scoop("x", 90)) // the feed reaches past the hour
        s.run(feed, com.bennybarak.scoops.rotter_scoops.ui.StringsEn)
        assertTrue(s.upToDate)
        assertTrue(!s.running)
        s.startOver()
        assertTrue(s.covered().isEmpty())
        assertEquals("[]", prefs.getString("digests"))
    }
    private fun scoop(id: String, minutesAgo: Int?) =
        Scoop(id, "t$id", "u", minutesAgo?.let { now - it * 60_000L })

    @Test fun windowIsByPostingTimeNewestFirst() {
        val s = DigestState(TestScope()) { now }
        // "old" may well have a fresh comment — it was still POSTED 3 hours ago.
        val feed = listOf(scoop("old", 180), scoop("b", 20), scoop("a", 4), scoop("none", null), scoop("c", 59))
        assertEquals(listOf("a", "b", "c"), s.inWindow(feed, 60, now).map { it.id })
        assertEquals(listOf("a"), s.inWindow(feed, 5, now).map { it.id })
        // The feed reaches back 3h: a 6h window needs older pages, a 1h one doesn't.
        assertTrue(s.needsOlder(feed, 360, now))
        assertTrue(!s.needsOlder(feed, 60, now))
    }

    @Test fun rangeIsFiveMinutesToADay() {
        assertEquals(5, DIGEST_STOPS.first())
        assertEquals(24 * 60, DIGEST_STOPS.last())
        assertEquals(DIGEST_STOPS.sorted(), DIGEST_STOPS)
    }

    @Test fun transcriptIsMainPostsOnlyAndCapped() {
        val items = (1..80).map { DigestItem("12:${it % 60}", "headline $it", "<b>" + "x".repeat(2000) + "</b>") }
        val t = ThreadSummarizer.digestTranscript(items)
        assertTrue(t.startsWith("[12:1] headline 1"))
        assertTrue(t.length <= 24_000)
        assertTrue(!t.contains("<b>"))
        // Every headline makes it in, even for a full day of scoops.
        assertTrue((1..80).all { t.contains("headline $it\n") })
    }
}
