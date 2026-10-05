package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.net.decodeWin1255
import com.bennybarak.scoops.rotter_scoops.net.decodeWin1255Percent
import com.bennybarak.scoops.rotter_scoops.net.encodeWin1255Form
import com.bennybarak.scoops.rotter_scoops.net.parseCardMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ParserTest {
    private fun fixture(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!.readBytes()

    private fun local(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        Calendar.getInstance().apply { clear(); set(y, mo - 1, d, h, mi) }.timeInMillis

    @Test fun win1255DecodesHebrewAndShekel() {
        // 0xE0..0xE2 = alef/bet/gimel; 0xA4 = ₪
        assertEquals("אבג", decodeWin1255(byteArrayOf(0xE0.toByte(), 0xE1.toByte(), 0xE2.toByte())))
        assertEquals("₪", decodeWin1255(byteArrayOf(0xA4.toByte())))
        assertEquals("AB", decodeWin1255(byteArrayOf(0x41, 0x42)))
    }

    @Test fun win1255PercentUsernames() {
        assertEquals("ארט", decodeWin1255Percent("%E0%F8%E8"))
        assertEquals("a b", decodeWin1255Percent("a+b"))
    }

    @Test fun win1255FormEncoding() {
        assertEquals("cmd=login&%F9%ED-%EE%F9%FA%EE%F9=a+b", encodeWin1255Form(listOf("cmd" to "login", "שם-משתמש" to "a b")))
    }

    @Test fun parseRssExtractsIdTitleDate() {
        val xml = """
<?xml version="1.0" encoding="windows-1255"?>
<rss version="0.91"><channel>
  <item>
    <pubDate>Sun, 21 Jun 2026 06:11:36 +0300</pubDate>
    <title>כותרת לדוגמה</title>
    <link>https://rotter.net/forum/scoops1/954033.shtml</link>
  </item>
</channel></rss>""".trim()
        val scoops = RotterService.parseRss(xml)
        assertEquals(1, scoops.size)
        assertEquals("954033", scoops[0].id)
        assertEquals("כותרת לדוגמה", scoops[0].title)
        // 06:11:36 +0300 == 03:11:36 UTC
        assertEquals(1782011496000L, scoops[0].published)
    }

    private val html = """
<html><body>
<table>
  <tr><td><a name="0"><b>שמעון</b></a></td></tr>
  <tr><td><h1 class="text16b">כותרת ראשית</h1></td></tr>
  <tr><td><font face="Arial" color="#000099"><font color="black">יום ראשון</font>
      <font color="red">06:11</font></font></td></tr>
  <tr bgcolor="#FDFDFD"><td>גוף ההודעה הראשית
    <script async data-telegram-post="N12chat/213825"></script>
    <a href="https://t.me/N12chat/213825">לחץ כאן</a></td></tr>
</table>
<table>
  <tr><td><a name="1"><b>לוי</b></a> <img src="/img/5_star.gif"></td></tr>
  <tr><td><font class="text16b">1. כותרת תגובה</font></td></tr>
  <tr><td><font color="#000099">21.06.26 <font color="red">06:14</font></font></td></tr>
  <tr bgcolor="#FDFDFD"><td><font class="text16b">1. כותרת תגובה</font>
    <a href="#0">בתגובה להודעה מספר 0</a>טקסט התגובה</td></tr>
</table>
</body></html>"""

    private val thread = RotterService.parseThread(html, "954033")

    @Test fun buildsRootAndCommentTree() {
        assertEquals(2, thread.messages.size)
        assertEquals(0, thread.root!!.num)
        assertEquals("שמעון", thread.root!!.author)
        assertEquals("כותרת ראשית", thread.root!!.title)
        assertEquals(0, thread.comments.single().parent)
    }

    @Test fun parsesRatingTitleTime() {
        val c = thread.comments.single()
        assertEquals("לוי", c.author)
        assertEquals(5, c.rating)
        assertEquals("כותרת תגובה", c.title)
        assertEquals("06:14", c.time)
        assertEquals(local(2026, 6, 21, 6, 14), c.timestamp)
    }

    @Test fun rootDateIsHebrewStringPlusRedTime() {
        assertEquals("06:11", thread.root!!.time)
        assertTrue(thread.root!!.date!!.contains("יום ראשון"))
    }

    @Test fun telegramScriptBecomesIframe() {
        val body = thread.root!!.bodyHtml!!
        assertTrue(body, body.contains("<iframe"))
        assertTrue(body, body.contains("t.me/N12chat/213825?embed=1"))
        assertTrue(body, body.contains("גוף ההודעה"))
    }

    @Test fun commentBodyStripsTitleAndReplyLine() {
        val body = thread.comments.single().bodyHtml!!
        assertTrue(body, body.contains("טקסט התגובה"))
        assertFalse(body, body.contains("בתגובה להודעה מספר"))
        assertFalse(body, body.contains("כותרת תגובה"))
    }

    // Real pages captured from rotter.net (raw cp1255 bytes). The thread has 59
    // messages, deep nesting and seven negative-points ("מינוס") members.
    private val captured by lazy { decodeWin1255(fixture("thread-960077.html")) }
    private val capturedThread by lazy { RotterService.parseThread(captured, "960077") }

    @Test fun everyCommentGetsGregorianTimestamp() {
        assertEquals(58, capturedThread.comments.size)
        assertTrue(capturedThread.comments.none { it.timestamp == null })
        assertTrue(Regex("""^\d{1,2}\.\d{1,2}\.\d{2}$""").matches(capturedThread.comments.first().date!!))
    }

    @Test fun negativePointsParsed() {
        assertEquals(7, capturedThread.messages.count { (it.points ?: 0) < 0 })
    }

    @Test fun cardMetaAgreesWithFullParse() {
        val meta = parseCardMeta(captured)
        val latest = capturedThread.comments.maxOf { it.timestamp!! }
        assertEquals(capturedThread.root!!.author, meta.author)
        assertEquals(capturedThread.root!!.points, meta.authorPoints)
        assertEquals(capturedThread.comments.size, meta.replies)
        assertEquals(latest, meta.lastComment)
    }

    @Test fun capturedBodiesAreClean() {
        for (m in capturedThread.messages) {
            val b = m.bodyHtml ?: continue
            assertFalse(b, b.startsWith("<br"))
            assertFalse(b, b.contains("<table"))
            assertNotNull(m.author)
        }
    }

    @Test fun capturedFeedParsesAll74Items() {
        val xml = decodeWin1255(fixture("rotternews.xml"))
        assertEquals(74, RotterService.parseRss(xml).size)
    }
}
