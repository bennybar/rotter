package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.data.Message
import com.bennybarak.scoops.rotter_scoops.data.Thread
import com.bennybarak.scoops.rotter_scoops.data.pruneOld
import com.bennybarak.scoops.rotter_scoops.net.encodeWin1255Form
import com.bennybarak.scoops.rotter_scoops.ui.widgets.EmbedBlock
import com.bennybarak.scoops.rotter_scoops.ui.widgets.HtmlBlocks
import com.bennybarak.scoops.rotter_scoops.ui.widgets.TextBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndependentReviewTest {
    // Emoji used to be posted as "?"; a browser sends a numeric reference.
    @Test fun emojiIsSentAsNumericReference() {
        // 😀 = U+1F600 = 128512 → "&#128512;" percent-encoded.
        assertEquals("body=%E0+%26%23128512%3B", encodeWin1255Form(listOf("body" to "א 😀")))
    }

    // A comment whose parent isn't on the page, or points at itself, or a
    // cycle, still appears (under the post) instead of vanishing.
    @Test fun orphanedCommentsHangOffThePost() {
        fun m(n: Int, p: Int?) = Message(num = n, author = "a", parent = p)
        val t = Thread("1", listOf(m(0, null), m(1, 0), m(2, 99), m(3, 3), m(4, 5), m(5, 4), m(6, 1)))
        assertEquals(listOf(1, 2, 3, 4, 5), t.childrenOf(0).map { it.num })
        assertEquals(listOf(6), t.childrenOf(1).map { it.num })
    }

    @Test fun historyIsBounded() {
        val ids = (0 until 2_500).map { "${966_000 - it * 20}" }.toMutableSet()
        pruneOld(ids)
        assertTrue(ids.all { it.toLong() >= 966_000 - 20_000 })
        assertTrue(ids.contains("966000"))
        val small = mutableSetOf("1", "966000")
        pruneOld(small) // under the size floor: untouched
        assertEquals(2, small.size)
    }

    // Embeds and links from posts: only real web URLs survive.
    @Test fun scriptUrlsAreDropped() {
        val base = "https://rotter.net/forum/scoops1/1.shtml"
        val blocks = HtmlBlocks.parse(
            "<iframe src=\"javascript:alert(1)\"></iframe><video src=\"data:x\"></video>" +
                "<a href=\"javascript:steal()\">x</a><iframe src=\"https://youtube.com/embed/a\"></iframe>",
            base,
            true,
        )
        val text = blocks.filterIsInstance<TextBlock>().single()
        assertNull(text.runs.single().link)
        assertEquals(listOf(EmbedBlock("https://youtube.com/embed/a")), blocks.filterIsInstance<EmbedBlock>())
    }
}
