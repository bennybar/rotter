package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.ui.screens.SummaryBlockKind
import com.bennybarak.scoops.rotter_scoops.ui.screens.isRtlText
import com.bennybarak.scoops.rotter_scoops.ui.screens.summaryBlocks
import com.bennybarak.scoops.rotter_scoops.ui.widgets.EmbedBlock
import com.bennybarak.scoops.rotter_scoops.ui.widgets.HtmlBlocks
import com.bennybarak.scoops.rotter_scoops.ui.widgets.ImageBlock
import com.bennybarak.scoops.rotter_scoops.ui.widgets.TelegramBlock
import com.bennybarak.scoops.rotter_scoops.ui.widgets.TextBlock
import com.bennybarak.scoops.rotter_scoops.ui.widgets.avatarColor
import com.bennybarak.scoops.rotter_scoops.ui.widgets.telegramChannelPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryTest {
    @Test fun splitsHeadingsBulletsNumberedAndParagraphs() {
        val b = summaryBlocks("## ההודעה\nשורה אחת\nוהמשך\n\n- נקודה **חשובה**\n2. שני")
        assertEquals(
            listOf(SummaryBlockKind.heading, SummaryBlockKind.paragraph, SummaryBlockKind.item, SummaryBlockKind.item),
            b.map { it.kind },
        )
        assertEquals("שורה אחת והמשך", b[1].text)
        assertEquals("2.", b[3].marker)
    }

    @Test fun direction() {
        assertTrue(isRtlText("OpenAI הודיעה על מודל חדש היום"))
        assertFalse(isRtlText("The Israeli site רוטר broke the story"))
    }

    @Test fun telegramLinks() {
        assertEquals("N12chat/213825", telegramChannelPost("https://t.me/N12chat/213825"))
        assertEquals("N12chat/213825", telegramChannelPost("https://t.me/N12chat/213825?embed=1"))
        assertNull(telegramChannelPost("https://t.me/s/N12chat/213825"))
        assertNull(telegramChannelPost("https://t.me/N12chat"))
        assertNull(telegramChannelPost("https://example.com/a/1"))
    }

    @Test fun bodyBlocks() {
        val html = "שורה <b>מודגשת</b><br>שנייה <a href=\"/x\">קישור</a>" +
            "<img src=\"/i.jpg\"><iframe src=\"https://t.me/c/5?embed=1\"></iframe>" +
            "<a href=\"https://t.me/c/5\">t</a><iframe src=\"https://youtube.com/watch?v=1\"></iframe>"
        val blocks = HtmlBlocks.parse(html, "https://rotter.net/forum/scoops1/1.shtml", true)
        val text = blocks[0] as TextBlock
        assertEquals("שורה מודגשת\nשנייה קישור", text.runs.joinToString("") { it.text })
        assertTrue(text.runs.any { it.bold && it.text == "מודגשת" })
        assertEquals("https://rotter.net/x", text.runs.first { it.link != null }.link)
        assertEquals(ImageBlock("https://rotter.net/i.jpg"), blocks[1])
        // The telegram iframe is dropped; the t.me link becomes the card.
        assertEquals(TelegramBlock("c/5"), blocks[2])
        assertEquals(EmbedBlock("https://youtube.com/watch?v=1"), blocks[3])
        assertEquals(4, blocks.size)
    }

    @Test fun telegramEmbedWithoutLinkBecomesCard() {
        val base = "https://rotter.net/forum/scoops1/1.shtml"
        // Only the embed (rotter's <script> turned iframe), no t.me link: one card.
        val only = HtmlBlocks.parse("טקסט<iframe src=\"https://t.me/c/7?embed=1\"></iframe>", base, true)
        assertEquals(TelegramBlock("c/7"), only[1])
        assertEquals(2, only.size)
        // Embed before its link: still exactly one card.
        val both = HtmlBlocks.parse("<iframe src=\"https://t.me/c/7?embed=1\"></iframe><a href=\"https://t.me/c/7\">x</a>", base, true)
        assertEquals(listOf(TelegramBlock("c/7")), both)
        // Inside a Telegram card nothing nests.
        assertTrue(HtmlBlocks.parse("<iframe src=\"https://t.me/c/7?embed=1\"></iframe>", "https://t.me/", false).isEmpty())
    }

    @Test fun avatarColorsMatchFlutterHash() {
        // Stable per name and spread across the palette.
        assertEquals(avatarColor("שמעון"), avatarColor("שמעון"))
        val distinct = listOf("שמעון", "לוי", "a", "b", "c", "d", "e", "f").map { avatarColor(it) }.toSet()
        assertTrue(distinct.size >= 4)
    }
}
