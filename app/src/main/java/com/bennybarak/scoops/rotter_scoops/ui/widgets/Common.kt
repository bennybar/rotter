package com.bennybarak.scoops.rotter_scoops.ui.widgets

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Flutter's HapticFeedback.selectionClick / mediumImpact, as Android maps them. */
fun View.selectionClick() = performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
fun View.mediumImpact() = performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

/**
 * A short, localized relative time ("just now" / "3h ago" / "לפני 3 שע׳").
 * Falls back to an absolute date for anything older than ~6 days.
 */
fun relTime(whenMs: Long, l: Strings, lang: String): String {
    val diff = System.currentTimeMillis() - whenMs
    val minutes = diff / 60_000
    if (diff < 0 || minutes < 1) return l.justNow
    if (minutes < 60) return l.minutesAgo(minutes.toInt())
    val hours = diff / 3_600_000
    if (hours < 24) return l.hoursAgo(hours.toInt())
    val days = diff / 86_400_000
    if (days <= 6) return l.daysAgo(days.toInt())
    return SimpleDateFormat("d MMM • HH:mm", Locale.forLanguageTag(lang)).format(Date(whenMs))
}

/**
 * A small, balanced palette so every author gets a stable identity colour for
 * their avatar (works on both the light/cream and dark backgrounds).
 */
private val avatarPalette = listOf(
    Color(0xFF3B82F6), // blue
    Color(0xFF10B981), // emerald
    Color(0xFFEC4899), // pink
    Color(0xFF8B5CF6), // violet
    Color(0xFFF59E0B), // amber
    Color(0xFF06B6D4), // cyan
    Color(0xFFEF6C4D), // coral
    Color(0xFF64748B), // slate
)

/**
 * Hashed by content with FNV-1a + a splitmix64 finaliser (the same function as
 * the Flutter build, so every author keeps their colour). The finaliser
 * avalanches the whole hash into the low bits `% 8` keeps.
 */
fun avatarColor(name: String): Color {
    var h = 0xcbf29ce484222325uL.toLong()
    for (c in name) h = (h xor c.code.toLong()) * 0x100000001b3L
    h = (h xor (h ushr 30)) * 0xbf58476d1ce4e5b9uL.toLong()
    h = (h xor (h ushr 27)) * 0x94d049bb133111ebuL.toLong()
    h = h xor (h ushr 31)
    return avatarPalette[Math.floorMod(h, avatarPalette.size.toLong()).toInt()]
}

/** A round, per-author identity bubble showing the first letter. */
@Composable
fun AvatarBubble(name: String, size: Dp, modifier: Modifier = Modifier) {
    val c = avatarColor(name)
    val t = name.trim()
    Box(
        modifier.size(size).background(c.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (t.isEmpty()) "?" else t.substring(0, 1),
            style = TextStyle(color = c, fontWeight = FontWeight.W800, fontSize = (size.value * 0.5f).sp),
        )
    }
}

/** Reputation-points pill (red when negative). Tapping opens the member profile. */
@Composable
fun PointsChip(pts: Int, muted: Color, label: String, onTap: (() -> Unit)?) {
    val c = if (pts < 0) Danger else muted
    Box(
        Modifier
            .semantics { contentDescription = "$label $pts" }
            .background(c.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        // LTR so a negative renders as "-3", not "3-" in the RTL thread.
        Text(
            "$pts",
            style = TextStyle(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.W800,
                color = c,
                textDirection = TextDirection.Ltr,
            ),
        )
    }
}

/** Open [url] in whatever app handles it (browser, Telegram, YouTube…). */
fun openExternal(context: Context, url: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

/** The system share sheet for plain text (a link or a summary). */
fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** A soft drop shadow like Flutter's BoxShadow (colour, blur radius, y offset). */
fun Modifier.dropShadowCompat(shape: Shape, color: Color, blur: Dp, offsetY: Dp): Modifier =
    dropShadow(shape, Shadow(radius = blur, color = color, offset = DpOffset(0.dp, offsetY)))
