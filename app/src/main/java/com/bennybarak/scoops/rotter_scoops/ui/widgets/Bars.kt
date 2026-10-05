package com.bennybarak.scoops.rotter_scoops.ui.widgets

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.NotoSansHebrew
import com.bennybarak.scoops.rotter_scoops.ui.palette

/** Height of the top bar (Flutter's kToolbarHeight). */
val BarHeight = 56.dp

val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 * The app bar: background-coloured, a 56dp row with an optional leading button,
 * a title starting 16dp in, and trailing actions.
 */
@Composable
fun AppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    titleSpacing: Int = 16,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val p = palette
    CompositionLocalProvider(LocalContentColor provides p.ink) {
        Row(
            modifier
                .fillMaxWidth()
                .height(BarHeight)
                .background(p.bg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { leading() }
            }
            Box(
                Modifier
                    .weight(1f)
                    .padding(start = titleSpacing.dp),
            ) { title() }
            actions()
            Box(Modifier.width(4.dp))
        }
    }
}

/** The bar title text (21sp, extra-bold). */
@Composable
fun BarTitle(text: String) {
    Text(
        text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = NotoSansHebrew,
            color = palette.ink,
            fontSize = 21.sp,
            fontWeight = FontWeight.W800,
            letterSpacing = (-0.3).sp,
        ),
    )
}

/** The default back button for pushed screens. */
@Composable
fun BackButton() {
    val nav = LocalNav.current
    IconButton(onClick = { nav.pop() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
    }
}

/**
 * A plain screen: status-bar-padded [bar] on top of [content], on the app
 * background. Content text defaults to the body colour.
 */
@Composable
fun AppScaffold(
    bar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val p = palette
    Column(
        modifier
            .fillMaxSize()
            .background(p.bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        bar()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalContentColor provides p.body) { content() }
        }
    }
}

/**
 * A screen whose top bar slides away when the user scrolls down and returns
 * when they scroll up. The bar is OVERLAID on top of the content (not stacked
 * above it), so hiding it never resizes the list; the content's scrollable
 * must reserve [BarHeight] of top padding so its first item isn't hidden.
 *
 * [atTop] reports whether the list is at its very top: the bar always shows
 * there, so it can never get stuck hidden.
 */
@Composable
fun ScrollHidingScaffold(
    bar: @Composable () -> Unit,
    atTop: () -> Boolean,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    barHeight: androidx.compose.ui.unit.Dp = BarHeight,
    floatingActions: (@Composable () -> Unit)? = null,
    floatingAlignment: Alignment = Alignment.BottomEnd,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = palette
    var visible by remember { mutableStateOf(true) }
    if (pinned && !visible) visible = true
    val threshold = with(LocalDensity.current) { 36.dp.toPx() }
    val accum = remember { mutableFloatStateOf(0f) }
    val pinnedNow by androidx.compose.runtime.rememberUpdatedState(pinned)
    val atTopNow by androidx.compose.runtime.rememberUpdatedState(atTop)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = -consumed.y // >0 = content moving up (reading down)
                if (pinnedNow || atTopNow()) {
                    visible = true
                    accum.floatValue = 0f
                    return Offset.Zero
                }
                if (delta == 0f) return Offset.Zero
                if ((delta > 0) != (accum.floatValue > 0)) accum.floatValue = 0f // direction flipped
                accum.floatValue += delta
                if (accum.floatValue > threshold && visible) {
                    visible = false
                    accum.floatValue = 0f
                } else if (accum.floatValue < -threshold && !visible) {
                    visible = true
                    accum.floatValue = 0f
                }
                return Offset.Zero
            }
        }
    }
    val barOffset by animateDpAsState(
        if (visible) 0.dp else -barHeight,
        animationSpec = tween(200, easing = EaseOutCubic),
        label = "bar",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(p.bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .clipToBounds(),
    ) {
        CompositionLocalProvider(LocalContentColor provides p.body) {
            Box(Modifier.fillMaxSize().nestedScroll(connection)) { content() }
        }
        Box(Modifier.fillMaxWidth().height(barHeight).offset(y = barOffset)) { bar() }
        if (floatingActions != null) {
            Box(
                Modifier
                    .align(floatingAlignment)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
            ) { floatingActions() }
        }
    }
}
