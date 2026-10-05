package com.bennybarak.scoops.rotter_scoops.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.data.Thread
import com.bennybarak.scoops.rotter_scoops.net.SummaryException
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BackButton
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.shareText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * An LLM summary of a thread. Only reachable when summaries are enabled and a
 * key is stored ([AIStore.isReady]).
 */
class SummaryState(val thread: Thread, val title: String, private val scope: CoroutineScope) {
    var summary by mutableStateOf<String?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(true); private set

    /** Set by the screen each composition. */
    var l: Strings? = null

    init {
        load()
    }

    fun load() {
        loading = true
        error = null
        scope.launch {
            val key = AIStore.apiKey()
            if (key == null) {
                loading = false
                error = l?.aiErrorNotConfigured
                return@launch
            }
            try {
                val text = ThreadSummarizer(
                    apiKey = key,
                    model = AIStore.model,
                    baseUrl = AIStore.baseUrl,
                    language = AIStore.promptLanguage(SettingsController.locale),
                ).summarize(thread, title)
                summary = text
                loading = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is SummaryException) e.message else (e.message ?: e.toString())
                loading = false
                error = if (msg.isEmpty()) l?.aiErrorEmpty else msg
            }
        }
    }
}

enum class SummaryBlockKind { heading, item, paragraph }

data class SummaryBlock(val kind: SummaryBlockKind, val text: String, val marker: String? = null) // "•" or "1."

private val headingRe = Regex("""^#{1,6}\s+(.*)$""")
private val bulletRe = Regex("""^[-*•]\s+(.*)$""")
private val numberedRe = Regex("""^(\d{1,2})[.)]\s+(.*)$""")

/**
 * Splits the model's Markdown into headings, list items and paragraphs — the
 * block structure plain text would otherwise show as literal `##` and `-`.
 */
fun summaryBlocks(markdown: String): List<SummaryBlock> {
    val out = ArrayList<SummaryBlock>()
    val para = ArrayList<String>()
    fun flush() {
        val joined = para.joinToString(" ").trim()
        para.clear()
        if (joined.isNotEmpty()) out.add(SummaryBlock(SummaryBlockKind.paragraph, joined))
    }
    for (raw in markdown.split('\n')) {
        val line = raw.trim()
        if (line.isEmpty()) {
            flush()
            continue
        }
        val heading = headingRe.find(line)
        val bullet = if (heading == null) bulletRe.find(line) else null
        val numbered = if (heading == null && bullet == null) numberedRe.find(line) else null
        when {
            heading != null -> {
                flush()
                out.add(SummaryBlock(SummaryBlockKind.heading, heading.groupValues[1]))
            }
            bullet != null -> {
                flush()
                out.add(SummaryBlock(SummaryBlockKind.item, bullet.groupValues[1], "•"))
            }
            numbered != null -> {
                flush()
                out.add(SummaryBlock(SummaryBlockKind.item, numbered.groupValues[2], "${numbered.groupValues[1]}."))
            }
            else -> para.add(line)
        }
    }
    flush()
    return out
}

/**
 * RTL unless the text is plainly left-to-right: predominantly Latin letters AND
 * opening with one. Majority alone mislabels a Hebrew line quoting a long
 * English name; first-strong alone mislabels a Hebrew line opening with a brand.
 */
fun isRtlText(text: String): Boolean {
    var rtl = 0
    var ltr = 0
    var firstRtl: Boolean? = null
    var i = 0
    while (i < text.length) {
        val r = text.codePointAt(i)
        i += Character.charCount(r)
        val isRtl = (r in 0x0590..0x08FF) || (r in 0xFB1D..0xFEFF)
        val isLtr = !isRtl && (r in 0x41..0x5A || r in 0x61..0x7A || r in 0xC0..0x24F)
        if (!isRtl && !isLtr) continue
        if (firstRtl == null) firstRtl = isRtl
        if (isRtl) rtl++ else ltr++
    }
    if (rtl > ltr || firstRtl == true) return true
    return firstRtl != false
}

/** `**bold**` spans; everything else is plain text. */
private val boldRe = Regex("""\*\*(.+?)\*\*""")

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in boldRe.findAll(text)) {
        if (m.range.first > last) append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.W800)) { append(m.groupValues[1]) }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

@Composable
private fun Directional(text: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtlText(text)) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

@Composable
fun SummaryScreen(s: SummaryState) {
    val l = strings
    val p = palette
    s.l = l
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    AppScaffold(
        bar = {
            AppBar(
                leading = { BackButton() },
                title = { BarTitle(l.aiSummary) },
                actions = {
                    val text = s.summary
                    if (text != null) {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(l.copy) }, onClick = {
                                    menu = false
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText(null, text))
                                    Snacks.show(l.copied, 1000)
                                })
                                DropdownMenuItem(text = { Text(l.share) }, onClick = {
                                    menu = false
                                    shareText(context, text)
                                })
                                HorizontalDivider()
                                // Explicit, and explicitly another billed request.
                                DropdownMenuItem(text = { Text(l.aiRegenerate) }, onClick = {
                                    menu = false
                                    s.load()
                                })
                            }
                        }
                    }
                },
            )
        },
    ) {
        if (s.loading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(14.dp))
                Text(l.aiSummarizing, style = TextStyle(color = p.muted))
            }
            return@AppScaffold
        }
        val summary = s.summary
        if (summary == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = p.muted, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(l.aiErrorTitle, style = TextStyle(fontWeight = FontWeight.W800, color = p.ink))
                    s.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = TextStyle(color = p.muted, textAlign = TextAlign.Center))
                    }
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(onClick = { s.load() }) { Text(l.retry) }
                }
            }
            return@AppScaffold
        }
        val body = TextStyle(fontSize = 15.5.sp, lineHeight = 1.5.em, color = p.ink)
        val blocks = remember(summary) { summaryBlocks(summary) }
        SelectionContainer {
            LazyColumn(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
            ) {
                item {
                    // Which thread this is about — the screen title only says "Summary".
                    Directional(s.title) {
                        Text(
                            s.title,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700, color = p.muted),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // Rendered as blocks: each takes its own direction, so a Hebrew
                // answer hugs the right and an English one the left.
                items(blocks.size) { i ->
                    val b = blocks[i]
                    Box(Modifier.padding(top = if (b.kind == SummaryBlockKind.heading) 14.dp else 6.dp)) {
                        Directional(b.text) {
                            when (b.kind) {
                                SummaryBlockKind.heading -> Text(
                                    inline(b.text),
                                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.W800, color = p.ink),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                SummaryBlockKind.item -> Row(Modifier.fillMaxWidth()) {
                                    Text(b.marker!!, style = body.copy(color = p.muted), modifier = Modifier.width(22.dp))
                                    Text(inline(b.text), style = body, modifier = Modifier.weight(1f))
                                }
                                SummaryBlockKind.paragraph -> Text(inline(b.text), style = body, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(18.dp))
                    // rotter runs on unverified rumour, and a fluent summary of a
                    // rumour reads far more authoritative than the thread itself.
                    Row {
                        Icon(Icons.Rounded.Info, null, tint = p.muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(l.aiDisclaimer, style = TextStyle(fontSize = 12.5.sp, color = p.muted), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
