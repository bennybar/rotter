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
    private fun scoop(id: String, minutesAgo: Int?) =
        Scoop(id, "t$id", "u", minutesAgo?.let { now - it * 60_000L })

    @Test fun windowIsByPostingTimeNewestFirst() {
        val s = DigestState(TestScope())
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
