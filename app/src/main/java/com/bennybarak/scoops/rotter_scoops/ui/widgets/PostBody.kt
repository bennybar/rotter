package com.bennybarak.scoops.rotter_scoops.ui.widgets

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import com.bennybarak.scoops.rotter_scoops.net.ROTTER_USER_AGENT
import com.bennybarak.scoops.rotter_scoops.net.getBytes
import com.bennybarak.scoops.rotter_scoops.ui.palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders a rotter message body as rich content: tappable links that open in
 * the right app, inline images (tap → full-screen zoomable viewer),
 * **natively rendered Telegram posts**, and lazy video/iframe embeds (YouTube)
 * that only spin up a real WebView when tapped. rotter's own inline styling is
 * stripped upstream, so the body uses the app's typography.
 *
 * [embedTelegram] is false when rendering the text INSIDE a Telegram card, so a
 * t.me link in the message doesn't recursively embed another card.
 */
@Composable
fun PostBody(
    html: String,
    baseUrl: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    embedTelegram: Boolean = true,
) {
    val blocks = remember(html, baseUrl, embedTelegram) { HtmlBlocks.parse(html, baseUrl, embedTelegram) }
    Column(modifier) {
        for (b in blocks) {
            when (b) {
                is TextBlock -> RichText(b, fontSize)
                is ImageBlock -> InlineImage(b.url)
                is EmbedBlock -> LazyEmbed(b.url)
                is TelegramBlock -> TelegramCard(b.channelPost)
            }
        }
    }
}

@Composable
private fun RichText(b: TextBlock, fontSize: Float) {
    val p = palette
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val text: AnnotatedString = remember(b, accent) {
        val linkStyle = TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline))
        buildAnnotatedString {
            for (r in b.runs) {
                val style = SpanStyle(
                    fontWeight = if (r.bold) FontWeight.W700 else null,
                    fontStyle = if (r.italic) FontStyle.Italic else null,
                    textDecoration = if (r.underline) TextDecoration.Underline else null,
                )
                if (r.link != null) {
                    val url = r.link
                    withLink(LinkAnnotation.Clickable(url, linkStyle) { openExternal(context, url) }) {
                        withStyle(style) { append(r.text) }
                    }
                } else {
                    withStyle(style) { append(r.text) }
                }
            }
        }
    }
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize.sp,
            lineHeight = 1.45.em,
            color = p.ink,
            textDirection = TextDirection.Rtl,
            textAlign = if (b.center) TextAlign.Center else TextAlign.Start,
        ),
    )
}

@Composable
private fun InlineImage(url: String) {
    val p = palette
    var viewing by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxW = maxWidth
        val maxPx = with(LocalDensity.current) { maxW.roundToPx() }
        val context = LocalContext.current
        val painter = rememberAsyncImagePainter(
            remember(url, maxPx) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(Dimension(maxPx), Dimension.Undefined))
                    .build()
            },
        )
        when (val s = painter.state) {
            is AsyncImagePainter.State.Success -> {
                val intrinsic = s.painter.intrinsicSize
                // Image pixels map 1:1 to dp (as Flutter's logical pixels did),
                // capped at the column width.
                val w = minOf(intrinsic.width, maxW.value).dp
                val h = w * (intrinsic.height / intrinsic.width)
                Image(
                    painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(w)
                        .height(h)
                        .clickable { viewing = true },
                )
            }
            is AsyncImagePainter.State.Error -> Unit
            else -> {
                // Draw the painter (that's what starts the load) at a modest
                // placeholder height.
                Image(
                    painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(p.muted.copy(alpha = 0.08f)),
                )
            }
        }
    }
    if (viewing) ImageViewer(url) { viewing = false }
}

/** Full-screen, pinch-to-zoom image viewer with tap/swipe-down to dismiss. */
@Composable
fun ImageViewer(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Image(
                rememberAsyncImagePainter(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
                    .pointerInput(Unit) {
                        var drag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = { if (scale <= 1f && drag > 120f) onDismiss() },
                        ) { _, dy -> drag += kotlin.math.abs(dy) }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(8.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

// ---- Telegram ---------------------------------------------------------------

/** A Telegram post scraped from its public `?embed=1` page. */
private class TelegramPost(val channel: String, val textHtml: String, val photoUrl: String?)

/**
 * Fetched posts, kept for the app's lifetime so scrolling never re-fetches.
 * A cached empty value means "we tried and it isn't available" (don't retry).
 */
private val tgCache = ConcurrentHashMap<String, Result<TelegramPost?>>()
private val photoRe = Regex("""background-image:\s*url\('([^']+)'\)""")

private suspend fun fetchTelegram(channelPost: String): TelegramPost? {
    tgCache[channelPost]?.let { return it.getOrNull() }
    val post = try {
        val bytes = getBytes(
            "https://t.me/$channelPost?embed=1",
            timeoutSeconds = 12,
            headers = mapOf("User-Agent" to ROTTER_USER_AGENT),
        )
        withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(String(bytes, Charsets.UTF_8))
            doc.outputSettings().prettyPrint(false)
            val text = doc.selectFirst(".tgme_widget_message_text")?.html() ?: ""
            val channel = doc.selectFirst(".tgme_widget_message_owner_name")?.wholeText()?.trim()
                ?: channelPost.split('/').first()
            val style = doc.selectFirst(".tgme_widget_message_photo_wrap")?.attr("style")
            val photo = style?.let { photoRe.find(it)?.groupValues?.get(1) }
            if (text.isEmpty() && photo == null) null else TelegramPost(channel, text, photo)
        }
    } catch (_: Exception) {
        null
    }
    tgCache[channelPost] = Result.success(post)
    return post
}

private val TelegramBlue = Color(0xFF2AABEE)

/**
 * A Telegram post rendered with NATIVE composables (no WebView in the scrolling
 * list). Tapping opens Telegram.
 */
@Composable
private fun TelegramCard(channelPost: String) {
    val p = palette
    val context = LocalContext.current
    var loading by remember(channelPost) { mutableStateOf(!tgCache.containsKey(channelPost)) }
    var post by remember(channelPost) { mutableStateOf(tgCache[channelPost]?.getOrNull()) }
    LaunchedEffect(channelPost) {
        if (loading) {
            post = fetchTelegram(channelPost)
            loading = false
        }
    }
    val open = { openExternal(context, "https://t.me/$channelPost"); Unit }
    Box(Modifier.padding(vertical = 8.dp)) {
        if (loading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(p.field, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
            return@Box
        }
        val tg = post
        // Couldn't load → just a plain link out to Telegram.
        if (tg == null) {
            Row(Modifier.clickable(onClick = open), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = TelegramBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "t.me/$channelPost",
                    style = TextStyle(color = TelegramBlue, textDecoration = TextDecoration.Underline, fontSize = 14.sp),
                )
            }
            return@Box
        }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = open)
                .background(p.field)
                .drawBehind {
                    // A blue edge on the start side.
                    val w = 3.dp.toPx()
                    drawRect(
                        TelegramBlue,
                        topLeft = Offset(if (rtl) size.width - w else 0f, 0f),
                        size = androidx.compose.ui.geometry.Size(w, size.height),
                    )
                }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = TelegramBlue, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    tg.channel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontWeight = FontWeight.W800, fontSize = 13.sp, color = TelegramBlue),
                )
            }
            if (tg.photoUrl != null) {
                Spacer(Modifier.height(8.dp))
                val painter = rememberAsyncImagePainter(tg.photoUrl)
                when (val s = painter.state) {
                    is AsyncImagePainter.State.Success -> {
                        val sz = s.painter.intrinsicSize
                        Image(
                            painter,
                            null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (sz.width > 0 && sz.height > 0) {
                                        Modifier.aspectRatio(sz.width / sz.height)
                                    } else Modifier,
                                ),
                        )
                    }
                    is AsyncImagePainter.State.Error -> Unit
                    // Don't jump the layout while the photo streams in.
                    else -> Image(
                        painter,
                        null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.muted.copy(alpha = 0.08f)),
                    )
                }
            }
            if (tg.textHtml.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PostBody(tg.textHtml, baseUrl = "https://t.me/", fontSize = 14.5f, embedTelegram = false)
            }
        }
    }
}

// ---- Video / iframe embeds ----------------------------------------------------

/** True for YouTube watch/share/shorts/embed URLs. */
private fun isYouTube(url: String): Boolean {
    val h = try {
        URI(url).host?.replaceFirst("www.", "") ?: ""
    } catch (_: Exception) {
        ""
    }
    return h == "youtu.be" || h.endsWith("youtube.com") || h.endsWith("youtube-nocookie.com")
}

/**
 * Normalize a YouTube URL to its embeddable `/embed/<id>` form (watch URLs are
 * refused inside an iframe; only `/embed/` plays).
 */
private fun normalizeEmbedUrl(url: String): String {
    val u = try {
        URI(url)
    } catch (_: Exception) {
        return url
    }
    val host = (u.host ?: return url).replaceFirst("www.", "")
    val segs = (u.path ?: "").split('/').filter { it.isNotEmpty() }
    var id: String? = null
    if (host == "youtu.be") {
        id = segs.firstOrNull()
    } else if (host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) {
        if (segs.firstOrNull() == "embed") return url
        id = u.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }?.substring(2)
        if (id == null && segs.size >= 2 && segs.first() == "shorts") id = segs[1]
    }
    return if (id == null) url else "https://www.youtube.com/embed/$id?playsinline=1&rel=0"
}

/**
 * A media embed that shows a lightweight placeholder until tapped; only then
 * does it create a real inline WebView (so a thread full of embeds stays light).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LazyEmbed(url: String) {
    val p = palette
    val accent = MaterialTheme.colorScheme.primary
    var loaded by remember(url) { mutableStateOf(false) }
    val youtube = remember(url) { isYouTube(url) }
    val src = remember(url) { if (youtube) normalizeEmbedUrl(url) else url }
    val host = remember(url) {
        try {
            URI(url).host?.replaceFirst("www.", "") ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    // 16:9 for video, a taller card for other embeds.
    val screenW = LocalConfiguration.current.screenWidthDp
    val width = (screenW - 64).toFloat().coerceIn(220f, 720f)
    val height = if (youtube) width * 9 / 16 else 460f
    Box(
        Modifier
            .padding(vertical = 6.dp)
            .width(width.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(14.dp)),
    ) {
        if (loaded) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webChromeClient = WebChromeClient()
                        // Hosted in a page on rotter.net so YouTube sees a real
                        // embedding origin (a bare player URL is refused).
                        loadDataWithBaseURL(
                            "https://rotter.net/",
                            "<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'>" +
                                "<style>html,body{margin:0;height:100%;background:transparent}</style></head><body>" +
                                "<iframe src=\"${src.replace("\"", "&quot;")}\" style=\"border:0;width:100%;height:100%\" " +
                                "allow=\"autoplay; encrypted-media; picture-in-picture; fullscreen\" allowfullscreen></iframe>" +
                                "</body></html>",
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                },
                onRelease = { it.destroy() },
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(p.field)
                    .clickable { loaded = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(56.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(host, style = TextStyle(color = p.muted, fontWeight = FontWeight.W700, fontSize = 13.sp))
            }
        }
    }
}
