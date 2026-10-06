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

    @Test fun runtimeIsKeptAndShown() {
        val prefs = com.bennybarak.scoops.rotter_scoops.data.Prefs
        prefs.setString("digests", """[{"at":$now,"minutes":60,"ids":["a"],"text":"## x","loadMs":2100,"aiMs":12140}]""")
        val d = DigestState(TestScope()) { now }.kept.single()
        assertEquals(2100L, d.loadMs)
        assertEquals("Took 14.2 s (loading 2.1 s · AI 12.1 s)", com.bennybarak.scoops.rotter_scoops.ui.StringsEn.digestRuntime(d.loadMs + d.aiMs, d.loadMs, d.aiMs))
    }

    @Test fun referencesAreHiddenAndParsed() {
        val (text, refs) = com.bennybarak.scoops.rotter_scoops.net.splitRefs("12:04 **פיצוץ** בצפון [3, 7]")
        assertEquals("12:04 **פיצוץ** בצפון", text)
        assertEquals(listOf(3, 7), refs)
        assertEquals("שקט" to emptyList<Int>(), com.bennybarak.scoops.rotter_scoops.net.splitRefs("שקט"))
        val t = ThreadSummarizer.digestTranscript(listOf(DigestItem("12:00", "a", null), DigestItem("12:05", "b", null)))
        assertTrue(t.startsWith("[1] 12:00 a") && t.contains("[2] 12:05 b"))
    }

    // A digest made for the last hour, viewed for the last 15 minutes, shows
    // only the lines about scoops posted in those 15 minutes.
    @Test fun shownLinesFollowTheSelectedWindow() {
        val m = 60_000L
        val src = listOf(
            com.bennybarak.scoops.rotter_scoops.net.DigestSource(1, "a", "A", now - 5 * m),
            com.bennybarak.scoops.rotter_scoops.net.DigestSource(2, "b", "B", now - 50 * m),
        )
        val d = com.bennybarak.scoops.rotter_scoops.ui.screens.KeptDigest(
            now - 2 * m, 60, setOf("a", "b"),
            "## North\n- recent [1]\n## South\n- older [2]\n- both [1, 2]", sources = src,
        )
        fun texts(since: Long) = com.bennybarak.scoops.rotter_scoops.ui.screens.visibleBlocks(d, since).map { it.text }
        assertEquals(listOf("North", "recent [1]", "South", "both [1, 2]"), texts(now - 15 * m))
        assertEquals(5, texts(now - 60 * m).size)
        assertTrue(texts(now - 1 * m).isEmpty())
        // Persisted with the digest, so taps still open threads after a restart.
        val prefs = com.bennybarak.scoops.rotter_scoops.data.Prefs
        prefs.setString("digests", "[]")
        val state = DigestState(TestScope()) { now }
        assertTrue(state.kept.isEmpty())
    }

    // "New only" shows just the newest run, and is remembered.
    @Test fun newOnlyShowsTheLatestRunAndPersists() {
        val prefs = com.bennybarak.scoops.rotter_scoops.data.Prefs
        prefs.setString(
            "digests",
            """[{"at":${now - 60_000},"minutes":60,"ids":["b"],"text":"## new"},""" +
                """{"at":${now - 3_600_000},"minutes":60,"ids":["a"],"text":"## old"}]""",
        )
        val s = DigestState(TestScope()) { now }
        assertEquals(2, s.considered().size)
        s.setNewOnlyAndSave(true)
        assertEquals(listOf("## new"), s.considered().map { it.text })
        assertTrue(DigestState(TestScope()) { now }.newOnly) // kept for next time
        s.setNewOnlyAndSave(false)
        assertTrue(!DigestState(TestScope()) { now }.newOnly)
    }

    @Test fun rangeIsFiveMinutesToADay() {
        assertEquals(5, DIGEST_STOPS.first())
        assertEquals(24 * 60, DIGEST_STOPS.last())
        assertEquals(DIGEST_STOPS.sorted(), DIGEST_STOPS)
    }

    @Test fun transcriptIsMainPostsOnlyAndCapped() {
        val items = (1..80).map { DigestItem("12:${it % 60}", "headline $it", "<b>" + "x".repeat(2000) + "</b>") }
        val t = ThreadSummarizer.digestTranscript(items)
        assertTrue(t.startsWith("[1] 12:1 headline 1"))
        assertTrue(t.length <= 24_000)
        assertTrue(!t.contains("<b>"))
        // Every headline makes it in, even for a full day of scoops.
        assertTrue((1..80).all { t.contains("headline $it\n") })
    }
}
