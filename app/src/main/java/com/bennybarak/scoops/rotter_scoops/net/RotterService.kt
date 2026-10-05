package com.bennybarak.scoops.rotter_scoops.net

import com.bennybarak.scoops.rotter_scoops.data.Message
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.util.Calendar
import java.util.TimeZone

/**
 * Reads rotter.net the browserless way: RSS for the list, `.shtml` for threads.
 * Everything on rotter is windows-1255 — we always decode raw bytes ourselves.
 * Decoding + parsing run on [Dispatchers.Default]: a thread page is
 * 100–400KB, and building its DOM on the main thread drops frames.
 */
object RotterService {
    private const val RSS_URL = "https://rotter.net/rss/rotternews.xml"
    fun threadUrl(id: String) = "https://rotter.net/forum/scoops1/$id.shtml"

    // ---- Post list (RSS) ----------------------------------------------------

    /**
     * The feed request the app starts at launch, before any UI exists; the
     * list's first load takes it instead of starting its own.
     */
    @Volatile
    var launchFeed: kotlinx.coroutines.Deferred<List<Scoop>>? = null

    /** The launch-time request if one is waiting, else a fresh fetch. */
    suspend fun fetchScoopsPreferringLaunch(): List<Scoop> {
        val early = launchFeed
        launchFeed = null
        return early?.await() ?: fetchScoops()
    }

    suspend fun fetchScoops(): List<Scoop> {
        val bytes = getBytes(RSS_URL)
        return withContext(Dispatchers.Default) { parseRss(decodeWin1255(bytes)) }
    }

    private val shtmlId = Regex("""/(\d+)\.shtml""")

    /** Pure RSS → scoops parse (network-free; unit-testable). */
    fun parseRss(xml: String): List<Scoop> {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val out = ArrayList<Scoop>()
        for (item in doc.getElementsByTag("item")) {
            val link = item.child("link")?.wholeText()?.trim() ?: ""
            val id = shtmlId.find(link)?.groupValues?.get(1) ?: continue
            out.add(
                Scoop(
                    id = id,
                    // rotter escapes ';' as '\;' in RSS titles — unescape it.
                    title = decodeEntities(item.child("title")?.wholeText()?.trim() ?: "")
                        .replace("\\;", ";"),
                    url = link,
                    published = parseRfc822(item.child("pubDate")?.wholeText()?.trim()),
                ),
            )
        }
        return out
    }

    private fun Element.child(tag: String): Element? = children().firstOrNull { it.tagName() == tag }

    /**
     * Lightweight per-card metadata (root author + their points, reply count,
     * last-comment time) for list cards — the RSS doesn't carry these.
     */
    suspend fun fetchCardMeta(id: String): CardMeta {
        val bytes = getBytes(threadUrl(id))
        // Decode + scan OFF the main thread, otherwise each card drops frames.
        return withContext(Dispatchers.Default) { parseCardMeta(decodeWin1255(bytes)) }
    }

    // ---- Thread + comments (.shtml) -----------------------------------------

    suspend fun fetchThread(id: String): Thread {
        val bytes = getBytes(threadUrl(id))
        return withContext(Dispatchers.Default) { parseThread(decodeWin1255(bytes), id) }
    }

    private val digits = Regex("""^\d+$""")
    private val starRe = Regex("""(\d)_star""")
    private val joinRe = Regex("""חבר מתאריך\s*([\d.]+)""")

    /** Pure thread-HTML → tree parse (network-free; unit-testable). */
    fun parseThread(html: String, id: String): Thread {
        val doc = Jsoup.parse(html)
        doc.outputSettings().prettyPrint(false)
        val messages = ArrayList<Message>()

        for (a in doc.select("a[name]")) {
            val name = a.attr("name")
            if (!digits.matches(name)) continue

            val table = ancestorTable(a) ?: continue

            val replyTo = table.selectFirst("a[href^=\"#\"]")
            val (date, time) = dateTime(table)
            val th = table.html()
            messages.add(
                Message(
                    num = name.toInt(),
                    author = (a.selectFirst("b")?.wholeText() ?: a.wholeText()).trim(),
                    rating = starRe.find(th)?.groupValues?.get(1)?.toIntOrNull(),
                    // The star image links to the member's ratings/details page.
                    profileUrl = table.selectFirst("a[href*=\"view_user_ratings\"]")?.attr("href"),
                    // Member stats shown next to the author in the thread HTML.
                    joinDate = joinRe.find(th)?.groupValues?.get(1),
                    messages = parseStat("הודעות", th),
                    raters = parseStat("מדרגים", th),
                    points = parseStat("נקודות", th),
                    title = title(table, isRoot = name == "0"),
                    bodyHtml = bodyHtml(table),
                    date = date,
                    time = time,
                    timestamp = commentTimestamp(date, time),
                    parent = replyTo?.attr("href")?.replace("#", "")?.toIntOrNull(),
                ),
            )
        }

        // De-dup (anchors can repeat) keeping first seen, then order by num.
        val seen = HashSet<Int>()
        val unique = messages.filter { seen.add(it.num) }.sortedBy { it.num }
        return Thread(id, unique)
    }

    private val dateRe = Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{2,4})$""")
    private val timeRe = Regex("""^(\d{1,2}):(\d{2})$""")

    /**
     * Parse a comment's gregorian `DD.MM.YY` + `HH:MM` into local epoch millis.
     * (The root's date is a Hebrew calendar string and isn't parsed here — the
     * reader falls back to the RSS pubDate for the root.)
     */
    fun commentTimestamp(date: String?, time: String?): Long? {
        if (date == null) return null
        val d = dateRe.find(date.trim()) ?: return null
        val t = if (time == null) null else timeRe.find(time.trim())
        var year = d.groupValues[3].toInt()
        if (year < 100) year += 2000
        return try {
            Calendar.getInstance().apply {
                clear()
                isLenient = true
                set(
                    year,
                    d.groupValues[2].toInt() - 1,
                    d.groupValues[1].toInt(),
                    t?.groupValues?.get(1)?.toInt() ?: 0,
                    t?.groupValues?.get(2)?.toInt() ?: 0,
                )
            }.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun ancestorTable(e: Element): Element? {
        var cur = e.parent()
        while (cur != null) {
            if (cur.tagName() == "table") return cur
            cur = cur.parent()
        }
        return null
    }

    private val leadingNum = Regex("""^\d+\.\s*""")

    private fun title(table: Element, isRoot: Boolean): String? {
        val raw = table.selectFirst(if (isRoot) "h1.text16b" else ".text16b")?.wholeText()?.trim()
        if (raw.isNullOrEmpty()) return null
        // Comment titles are prefixed "N. " — drop the leading message number.
        return raw.replaceFirst(leadingNum, "").trim()
    }

    private val clockRe = Regex("""^\d{1,2}:\d{2}$""")
    private val gregorianRe = Regex("""\d{1,2}\.\d{1,2}\.\d{2,4}""")
    private val spaces = Regex("""\s+""")

    /**
     * The post date + time live in the header cell: the time is a red
     * `<font color="red">HH:MM</font>` (NOT the red "feedback" link, which also
     * exists), and the date is the gregorian DD.MM.YY in the same cell (falling
     * back to the text right before the time when there is none).
     */
    private fun dateTime(table: Element): Pair<String?, String?> {
        val timeEl = table.select("font[color]")
            .firstOrNull { it.attr("color") == "red" && clockRe.matches(it.wholeText().trim()) }
            ?: return null to null
        val time = timeEl.wholeText().trim()
        // Every header reads Hebrew date, time, then the gregorian date. Take the
        // gregorian DD.MM.YY from anywhere in the enclosing <td> — that scope
        // excludes the author's join date, which sits in the neighbouring cell.
        var cell: Element? = timeEl.parent()
        while (cell != null && cell.tagName() != "td" && cell.tagName() != "th") {
            cell = cell.parent()
        }
        val gregorian = gregorianRe.find(cell?.wholeText() ?: "")?.value
        if (gregorian != null) return gregorian to time
        val cellText = (timeEl.parent()?.wholeText() ?: "").replace(' ', ' ')
        val idx = cellText.indexOf(time)
        val before = (if (idx >= 0) cellText.substring(0, idx) else cellText)
            .replace(spaces, " ")
            .trim()
        return (before.ifEmpty { null }) to time
    }

    private val strippedAttrs = listOf(
        "color", "face", "size", "style", "align", "valign", "width", "height",
        "bgcolor", "cellpadding", "cellspacing", "border", "nowrap", "class",
    )
    private val htmlComment = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
    private val tableTags = Regex("""</?(table|tbody|thead|tr|td|th)[^>]*>""", RegexOption.IGNORE_CASE)
    private val pTags = Regex("""</?p[^>]*>""", RegexOption.IGNORE_CASE)
    private val bTags = Regex("""</?b>""", RegexOption.IGNORE_CASE)
    private val emptyWrappers = Regex(
        """<(div|center|font|span|b|i|u)[^>]*>(?:\s|<br\s*/?>)*</\1>""",
        RegexOption.IGNORE_CASE,
    )
    private val brRuns = Regex("""(?:\s*<br\s*/?>\s*){2,}""", RegexOption.IGNORE_CASE)
    private val leadBr = Regex(
        """^((?:\s|<(?!br)[a-zA-Z][^>]*>|</[a-zA-Z][^>]*>)*)<br\s*/?>\s*""",
        RegexOption.IGNORE_CASE,
    )
    private val trailBr = Regex("""<br\s*/?>\s*((?:\s|</[a-zA-Z][^>]*>)*)$""", RegexOption.IGNORE_CASE)

    /**
     * The message body as **cleaned HTML** — keeps links, images, and embedded
     * media (iframes / `<video>`, e.g. Telegram/YouTube) so the reader can render
     * them inline. Only ads, the title line, and the "in reply to N" line are
     * stripped. Returns null when there's no real text *and* no media.
     */
    private fun bodyHtml(table: Element): String? {
        val cell = table.selectFirst("tr[bgcolor=\"#FDFDFD\"] td") ?: return null
        // A detached clone serializes with the default (pretty-printing) settings,
        // so give it an owner document that doesn't reformat the markup.
        val holder = Document("").apply { outputSettings().prettyPrint(false) }
        val clone = cell.clone()
        holder.appendChild(clone)

        // Telegram embeds arrive as a widget <script data-telegram-post="chan/123">
        // (which can't run in the renderer). Swap each for an embeddable iframe.
        for (s in clone.select("script[data-telegram-post]")) {
            val post = s.attr("data-telegram-post")
            if (post.isEmpty()) continue
            val iframe = Element("iframe")
                .attr("src", "https://t.me/$post?embed=1")
                .attr("width", "100%")
                .attr("height", "480")
            s.replaceWith(iframe)
        }

        // Drop ads + the title line + the "in reply to message N" line.
        clone.select(
            "script, style, ins, noscript, .text16b, a[href^=\"#\"], " +
                "div[id*=\"gpt-ad\"], div[id*=\"taboola\"], " +
                "iframe[src*=\"doubleclick\"], iframe[src*=\"googlesyndication\"]",
        ).filter { it !== clone }.forEach { it.remove() }
        // Neutralize rotter's legacy presentational attributes + inline styles so
        // the body flows in the app's own typography. Structure, links and media stay.
        for (el in clone.select("*")) {
            if (el === clone) continue
            for (attr in strippedAttrs) el.removeAttr(attr)
        }

        val hasMedia = clone.selectFirst("img, iframe, video, embed, source") != null
        val text = clone.wholeText().trim()
        if (text.isEmpty() && !hasMedia) return null

        // Rotter wraps post text in layout <table>s and pads it with stray <br>
        // runs and empty paragraphs — unwrap the tables and collapse the breaks.
        var out = clone.html()
            .replace(htmlComment, "")
            .replace("&nbsp;", " ")
            .replace(tableTags, " ")
            .replace(pTags, "<br>")
            .replace(bTags, "")
            .replace(emptyWrappers, "")
            .replace(brRuns, "<br>")

        // Strip leading / trailing <br> even inside (or between) inline wrappers.
        while (true) {
            val m = leadBr.find(out) ?: break
            out = out.replaceRange(m.range, m.groupValues[1])
        }
        while (true) {
            val m = trailBr.find(out) ?: break
            out = out.replaceRange(m.range, m.groupValues[1])
        }
        return out.trim()
    }

    private fun decodeEntities(s: String): String = Jsoup.parseBodyFragment(s).body().wholeText()

    private val rfc822 = Regex("""(\d{1,2})\s+(\w{3})\s+(\d{4})\s+(\d{2}):(\d{2}):(\d{2})\s*([+-]\d{4})?""")
    private val months = mapOf(
        "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
        "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12,
    )

    /** e.g. "Fri, 19 Jun 2026 17:22:46 +0300" → epoch millis. */
    private fun parseRfc822(s: String?): Long? {
        if (s.isNullOrEmpty()) return null
        val m = rfc822.find(s) ?: return null
        val g = m.groupValues
        val month = months[g[2]] ?: return null
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(g[3].toInt(), month - 1, g[1].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt())
        }.timeInMillis
        // Apply the offset to get true UTC.
        val off = g[7]
        if (off.isNotEmpty()) {
            val sign = if (off[0] == '-') -1 else 1
            val h = off.substring(1, 3).toInt()
            val min = off.substring(3, 5).toInt()
            return utc - sign * (h * 3_600_000L + min * 60_000L)
        }
        return utc
    }
}

/**
 * Member stats read as `<n> <label>`. NEGATIVE points are written with the
 * Hebrew word "מינוס" before the number (e.g. "מינוס 3 נקודות" = -3) — AND
 * rotter closes a <b> between the number and the label, so tags are allowed
 * between them.
 */
private val statPatterns = java.util.concurrent.ConcurrentHashMap<String, Regex>()

fun parseStat(label: String, html: String): Int? {
    val re = statPatterns.getOrPut(label) {
        Regex("""(מינוס\s+)?([\d,]+)\s*(?:</?[^>]+>\s*)*""" + Regex.escape(label))
    }
    val m = re.find(html) ?: return null
    val n = m.groupValues[2].replace(",", "").toIntOrNull() ?: return null
    return if (m.groupValues[1].isNotEmpty()) -n else n
}

/** Per-card metadata the RSS doesn't carry. */
data class CardMeta(val author: String?, val authorPoints: Int?, val replies: Int, val lastComment: Long?)

private val redClockThenDate = Regex(
    """color=["']?red["']?[^>]*>\s*(\d{1,2}:\d{2})\s*</font>[\s\S]{0,300}?(\d{1,2}\.\d{1,2}\.\d{2,4})""",
    RegexOption.IGNORE_CASE,
)
// Anchors with or without quotes around the number (`name="0"` / `name=0`).
private val rootAnchor = Regex("""<a\s+name=["']?0["']?(?=[\s>])[^>]*>""", RegexOption.IGNORE_CASE)
private val nextAnchor = Regex("""<a\s+name=["']?\d""", RegexOption.IGNORE_CASE)
private val anchorNums = Regex("""<a\s+name=["']?(\d+)["']?(?=[\s>])""", RegexOption.IGNORE_CASE)
// The poster's name: the first text after the anchor, past any wrapping tags.
private val authorRe = Regex("""^\s*(?:<[^>]+>\s*)*([^<]+)""")

/**
 * Pulls the handful of facts a list card needs straight out of the raw HTML,
 * without building a DOM — opening the app does this for ~75 pages of
 * 50–400KB, and the full parse dominated. The unit tests pin the output against
 * [RotterService.parseThread] on a real captured thread.
 */
fun parseCardMeta(html: String): CardMeta {
    // The original post's slice: from its <a name="0"> anchor to the next
    // message's anchor, so the FIRST COMMENTER's points aren't read as the poster's.
    var root = ""
    val anchor = rootAnchor.find(html)
    if (anchor != null) {
        val rest = html.substring(anchor.range.last + 1)
        val next = nextAnchor.find(rest)
        root = if (next == null) rest else rest.substring(0, next.range.first)
    }
    val author = authorRe.find(root)?.groupValues?.get(1)?.trim()

    val nums = anchorNums.findAll(html).map { it.groupValues[1] }.toHashSet()

    // Every header is `…<font color=red>HH:MM</font> … DD.MM.YY…`: pair each red
    // clock with the gregorian date that follows it. The window is bounded so a
    // header missing its date can't pair with the next message's.
    var last: Long? = null
    for (m in redClockThenDate.findAll(html)) {
        val ts = RotterService.commentTimestamp(m.groupValues[2], m.groupValues[1])
        if (ts != null && (last == null || ts > last)) last = ts
    }

    return CardMeta(
        author = if (author.isNullOrEmpty()) null else author,
        authorPoints = parseStat("נקודות", root),
        replies = if (nums.contains("0")) nums.size - 1 else nums.size,
        lastComment = last,
    )
}
