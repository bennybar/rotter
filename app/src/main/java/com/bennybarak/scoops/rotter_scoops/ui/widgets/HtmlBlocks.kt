package com.bennybarak.scoops.rotter_scoops.ui.widgets

import android.util.LruCache
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/** One styled stretch of text inside a [TextBlock]. */
data class Run(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val link: String? = null,
)

/** A message body, split into the pieces the renderer lays out top to bottom. */
sealed interface Block
data class TextBlock(val runs: List<Run>, val center: Boolean) : Block
data class ImageBlock(val url: String) : Block
data class EmbedBlock(val url: String) : Block
data class TelegramBlock(val channelPost: String) : Block

/**
 * For a Telegram post link `t.me/<channel>/<id>` (id numeric) returns
 * "channel/id"; null for anything else (channel-only links, /s/ previews, …).
 */
fun telegramChannelPost(url: String?): String? {
    if (url == null) return null
    val u = try {
        URI(url.trim())
    } catch (_: Exception) {
        return null
    }
    val host = (u.host ?: return null).replaceFirst("www.", "")
    if (host != "t.me" && host != "telegram.me") return null
    val segs = (u.path ?: "").split('/').filter { it.isNotEmpty() }
    if (segs.size >= 2 && segs.first() != "s" && segs.last().all { it.isDigit() }) {
        return "${segs[segs.size - 2]}/${segs.last()}"
    }
    return null
}

/** Resolve [src] against [base] (relative image/iframe/link URLs on rotter). */
fun resolveUrl(base: String, src: String): String = try {
    URI(base).resolve(src.trim().replace(" ", "%20")).toString()
} catch (_: Exception) {
    src
}

/**
 * Turns a cleaned message body into [Block]s: flowing text (bold / italic /
 * underline / links, line breaks, `<center>`), inline images, Telegram post
 * cards for `t.me/<channel>/<id>` links, and tap-to-load embeds for iframes and
 * videos. Results are cached: comments are re-laid out as the list scrolls.
 */
object HtmlBlocks {
    private val cache = LruCache<String, List<Block>>(600)

    fun parse(html: String, baseUrl: String, embedTelegram: Boolean): List<Block> {
        val key = "$embedTelegram|$baseUrl|$html"
        cache.get(key)?.let { return it }
        val blocks = Builder(baseUrl, embedTelegram).run(html)
        cache.put(key, blocks)
        return blocks
    }

    private val blockTags = setOf(
        "div", "p", "center", "li", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "table", "tr", "section", "article", "header", "footer", "figure", "hr",
    )

    private class Builder(val baseUrl: String, val embedTelegram: Boolean) {
        val out = ArrayList<Block>()
        val runs = ArrayList<Run>()
        val text = StringBuilder() // mirror of runs' text, for whitespace decisions
        var center = false
        var bold = 0
        var italic = 0
        var underline = 0
        var centerDepth = 0
        val links = ArrayList<String>()

        /** Telegram posts linked from the text (they render as cards there). */
        val linkedPosts = HashSet<String>()

        fun run(html: String): List<Block> {
            val body = Jsoup.parseBodyFragment(html).body()
            if (embedTelegram) {
                for (a in body.select("a[href]")) telegramChannelPost(a.attr("href"))?.let(linkedPosts::add)
            }
            walk(body)
            flush()
            return out
        }

        private fun append(s: String) {
            if (s.isEmpty()) return
            val r = Run(s, bold > 0, italic > 0, underline > 0, links.lastOrNull())
            val last = runs.lastOrNull()
            if (last != null && last.copy(text = "") == r.copy(text = "")) {
                runs[runs.lastIndex] = last.copy(text = last.text + s)
            } else {
                runs.add(r)
            }
            text.append(s)
        }

        private fun atLineStart() = text.isEmpty() || text.last() == '\n'

        private fun newline() {
            // Drop a space dangling at the end of the line.
            if (text.isNotEmpty() && text.last() == ' ') trimTrailingSpace()
            append("\n")
        }

        private fun ensureLineBreak() {
            if (!atLineStart()) newline()
        }

        private fun trimTrailingSpace() {
            val last = runs.lastOrNull() ?: return
            if (last.text.endsWith(" ")) {
                val t = last.text.dropLast(1)
                if (t.isEmpty()) runs.removeAt(runs.lastIndex) else runs[runs.lastIndex] = last.copy(text = t)
                text.setLength(text.length - 1)
            }
        }

        private fun text(raw: String) {
            // HTML whitespace collapsing.
            val sb = StringBuilder()
            var prevSpace = text.isEmpty() || text.last() == ' ' || text.last() == '\n'
            for (ch in raw) {
                if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r' || ch == '\u000C') {
                    if (!prevSpace) sb.append(' ')
                    prevSpace = true
                } else {
                    sb.append(ch)
                    prevSpace = false
                }
            }
            append(sb.toString())
        }

        fun flush() {
            // Trim surrounding whitespace / blank lines off the paragraph.
            var all = runs.joinToString("") { it.text }
            val lead = all.length - all.trimStart().length
            val trail = all.length - all.trimEnd().length
            if (all.isNotBlank()) {
                val trimmed = ArrayList<Run>()
                var pos = 0
                val end = all.length - trail
                for (r in runs) {
                    val s = maxOf(pos, lead)
                    val e = minOf(pos + r.text.length, end)
                    if (e > s) trimmed.add(r.copy(text = r.text.substring(s - pos, e - pos)))
                    pos += r.text.length
                }
                out.add(TextBlock(trimmed, center))
            }
            all = ""
            runs.clear()
            text.setLength(0)
        }

        private fun switchCenter(c: Boolean) {
            if (c == center) return
            flush()
            center = c
        }

        fun walk(node: Node) {
            for (child in node.childNodes()) {
                when (child) {
                    is TextNode -> {
                        switchCenter(centerDepth > 0)
                        text(child.wholeText)
                    }
                    is Element -> element(child)
                }
            }
        }

        private fun element(e: Element) {
            when (val tag = e.tagName()) {
                "br" -> {
                    switchCenter(centerDepth > 0)
                    newline()
                }
                "img" -> {
                    val src = e.attr("src")
                    if (src.isNotBlank() && !src.startsWith("data:")) {
                        flush()
                        out.add(ImageBlock(resolveUrl(baseUrl, src)))
                    }
                }
                "iframe", "video" -> {
                    val src = e.attr("src").ifBlank { e.selectFirst("source")?.attr("src") ?: "" }
                    if (src.isNotBlank()) {
                        val abs = resolveUrl(baseUrl, src)
                        val cp = telegramChannelPost(abs)
                        if (cp == null) {
                            flush()
                            out.add(EmbedBlock(abs))
                        } else if (embedTelegram && linkedPosts.add(cp)) {
                            // A Telegram embed with no t.me link in the text: show
                            // it as the card. (When the link is there, the link
                            // renders the card and this duplicate is dropped.)
                            flush()
                            out.add(TelegramBlock(cp))
                        }
                    }
                }
                "script", "style", "noscript", "embed", "object", "source" -> Unit
                "a" -> {
                    val href = e.attr("href")
                    val cp = if (embedTelegram) telegramChannelPost(href) else null
                    if (cp != null) {
                        flush()
                        out.add(TelegramBlock(cp))
                        return
                    }
                    if (href.isNotBlank()) links.add(resolveUrl(baseUrl, href))
                    walk(e)
                    if (href.isNotBlank()) links.removeAt(links.lastIndex)
                }
                "b", "strong" -> {
                    bold++; walk(e); bold--
                }
                "i", "em", "cite" -> {
                    italic++; walk(e); italic--
                }
                "u", "ins" -> {
                    underline++; walk(e); underline--
                }
                else -> {
                    val isBlock = tag in blockTags
                    val isHeading = tag.length == 2 && tag[0] == 'h' && tag[1].isDigit()
                    if (tag == "center") centerDepth++
                    if (isBlock) ensureLineBreak()
                    if (tag == "li") append("• ")
                    if (isHeading) bold++
                    walk(e)
                    if (isHeading) bold--
                    if (tag == "center") centerDepth--
                    if (isBlock) ensureLineBreak()
                }
            }
        }
    }
}
