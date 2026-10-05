package com.bennybarak.scoops.rotter_scoops.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.bennybarak.scoops.rotter_scoops.data.Accent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.R
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.data.ThemeMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import java.util.Locale

/** Bundled Hebrew-first typeface — no runtime download. */
val NotoSansHebrew = FontFamily(
    Font(R.font.noto_sans_hebrew_400, FontWeight.W400),
    Font(R.font.noto_sans_hebrew_500, FontWeight.W500),
    Font(R.font.noto_sans_hebrew_600, FontWeight.W600),
    Font(R.font.noto_sans_hebrew_700, FontWeight.W700),
    Font(R.font.noto_sans_hebrew_800, FontWeight.W800),
)

/** Neutral ink/background ramps (per colour theme). */
@Immutable
data class Palette(
    val dark: Boolean,
    val ink: Color,
    val muted: Color,
    val bg: Color,
    val surface: Color,
    val field: Color,
    val body: Color,
)

private val LightPalette = Palette(
    dark = false,
    ink = Color(0xFF15130E),
    muted = Color(0xFF8A8378),
    bg = Color(0xFFF6F4F0),
    surface = Color.White,
    field = Color(0xFFECE9E3),
    body = Color(0xFF3C382F),
)

private val DarkPalette = Palette(
    dark = true,
    ink = Color(0xFFF0ECE4),
    muted = Color(0xFFA39C90),
    bg = Color(0xFF141310),
    surface = Color(0xFF1E1C18),
    field = Color(0xFF28251F),
    body = Color(0xFFCFCABF),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalStrings = staticCompositionLocalOf<Strings> { StringsHe }

/** The interface direction (follows the app language); content is always RTL. */
val LocalChromeDirection = staticCompositionLocalOf { LayoutDirection.Rtl }

/** The effective app language code ("he" / "en"). */
val LocalLanguage = staticCompositionLocalOf { "he" }

val palette: Palette @Composable get() = LocalPalette.current
val strings: Strings @Composable get() = LocalStrings.current

/** Green used for "my reply" markers (Material green.shade600). */
val Mine = Color(0xFF43A047)

/** Material red.shade400, for destructive / negative accents. */
val Danger = Color(0xFFEF5350)

private fun typography(p: Palette): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = NotoSansHebrew)
    return Typography(
        displayLarge = base.displayLarge.f(),
        displayMedium = base.displayMedium.f(),
        displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(),
        headlineMedium = base.headlineMedium.f()
            .copy(fontWeight = FontWeight.W800, letterSpacing = (-0.5).sp, color = p.ink),
        headlineSmall = base.headlineSmall.f()
            .copy(fontWeight = FontWeight.W800, letterSpacing = (-0.3).sp, color = p.ink),
        titleLarge = base.titleLarge.f()
            .copy(fontWeight = FontWeight.W700, letterSpacing = (-0.2).sp, color = p.ink),
        titleMedium = base.titleMedium.f().copy(fontWeight = FontWeight.W700, color = p.ink),
        titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(),
        bodyMedium = base.bodyMedium.f().copy(color = p.body),
        bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(),
        labelMedium = base.labelMedium.f(),
        labelSmall = base.labelSmall.f(),
    )
}

/** The text style every plain Text starts from (Flutter's DefaultTextStyle: bodyMedium). */
private val BaseText = TextStyle(
    fontFamily = NotoSansHebrew,
    fontSize = 14.sp,
    lineHeight = 1.43.em,
    letterSpacing = 0.25.sp,
)

/**
 * The palette and Material colour scheme for a colour theme. Classic amber
 * keeps the original hand-tuned warm neutrals and the pure accent as primary.
 * Every other theme takes its neutrals from the theme's own tonal palette, so
 * backgrounds, cards and fields carry its hue, with the scheme's primary.
 */
fun themeColors(accent: Accent, dark: Boolean): Pair<Palette, ColorScheme> {
    val seed = Color(accent.seed)
    val base = dynamicColorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
    )
    if (accent == Accent.amber) {
        val p = if (dark) DarkPalette else LightPalette
        return p to base.copy(
            primary = seed,
            // The scheme's own onPrimary is near-black in dark mode, unreadable
            // on this saturated accent; white reads on it.
            onPrimary = Color.White,
            surface = p.surface,
            onSurface = p.ink,
            onSurfaceVariant = p.muted,
            background = p.bg,
        )
    }
    val p = Palette(
        dark = dark,
        ink = base.onSurface,
        muted = base.onSurfaceVariant.copy(alpha = 0.85f).compositeOver(if (dark) base.surface else base.surfaceContainer),
        // Light: cards a step lighter than the page; dark: a step lighter too.
        bg = if (dark) base.surface else base.surfaceContainer,
        surface = if (dark) base.surfaceContainerHigh else base.surfaceContainerLowest,
        field = if (dark) base.surfaceContainerHighest else base.surfaceContainerHigh,
        body = base.onSurfaceVariant,
    )
    return p to base.copy(surface = p.surface, background = p.bg)
}

@Composable
fun ScoopsTheme(content: @Composable () -> Unit) {
    val s = SettingsController
    val dark = when (s.mode) {
        ThemeMode.light -> false
        ThemeMode.dark -> true
        ThemeMode.system -> isSystemInDarkTheme()
    }
    val (p, scheme) = remember(s.accent, dark) { themeColors(s.accent, dark) }
    // Chrome follows the language and reads naturally; the Hebrew *content* is
    // wrapped RTL at the screen level.
    val lang = s.locale ?: Locale.getDefault().language.let { if (it == "iw" || it == "he") "he" else "en" }
    val str = if (lang == "he") StringsHe else StringsEn
    val direction = if (lang == "he") LayoutDirection.Rtl else LayoutDirection.Ltr
    // The text-size setting is applied on top of the device's own scaling.
    val density = LocalDensity.current
    val scaled = remember(density, s.textScale) {
        Density(density.density, density.fontScale * s.textScale.toFloat())
    }
    MaterialTheme(colorScheme = scheme, typography = remember(p) { typography(p) }) {
        CompositionLocalProvider(
            LocalPalette provides p,
            LocalStrings provides str,
            LocalLanguage provides lang,
            LocalChromeDirection provides direction,
            LocalLayoutDirection provides direction,
            LocalDensity provides scaled,
            LocalTextStyle provides BaseText,
            LocalContentColor provides p.body,
            content = content,
        )
    }
}
