package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRightAlt
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DensityLarge
import androidx.compose.material.icons.rounded.DensityMedium
import androidx.compose.material.icons.rounded.DensitySmall
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.Accent
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.data.ThemeMode
import com.bennybarak.scoops.rotter_scoops.ui.AISettingsRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.openLogin
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.dropShadowCompat
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(bottomInset: Dp) {
    val l = strings
    val p = palette
    val nav = LocalNav.current
    val s = SettingsController
    val accent = MaterialTheme.colorScheme.primary
    var confirmSignOut by remember { mutableStateOf(false) }
    // Signing out erases the saved credentials and "my replies": ask first.
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            text = { Text(l.signOutConfirm) },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(l.cancel) } },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        nav.home.scope.launch { AuthService.signOut() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                ) { Text(l.signOut) }
            },
        )
    }
    AppScaffold(bar = { AppBar(title = { BarTitle(l.settingsTitle) }) }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp + bottomInset),
        ) {
            item {
                SectionLabel(l.appearance)
                Spacer(Modifier.height(10.dp))
                Card {
                    Column(Modifier.padding(16.dp)) {
                        RowLabel(Icons.Rounded.DarkMode, l.theme)
                        Spacer(Modifier.height(12.dp))
                        Segmented(
                            listOf(
                                Triple(Icons.Rounded.LightMode, l.themeLight, s.mode == ThemeMode.light),
                                Triple(Icons.Rounded.DarkMode, l.themeDark, s.mode == ThemeMode.dark),
                                Triple(Icons.Rounded.PhoneIphone, l.themeSystem, s.mode == ThemeMode.system),
                            ),
                        ) { i -> s.setMode(listOf(ThemeMode.light, ThemeMode.dark, ThemeMode.system)[i]) }
                        Spacer(Modifier.height(22.dp))
                        RowLabel(Icons.Rounded.Palette, l.accentColor)
                        Spacer(Modifier.height(14.dp))
                        AccentPicker(s.accent)
                        Spacer(Modifier.height(22.dp))
                        RowLabel(Icons.Rounded.FormatSize, l.textSize)
                        Spacer(Modifier.height(4.dp))
                        StepSlider(
                            value = s.textScale,
                            min = SettingsController.MIN_SCALE,
                            max = SettingsController.MAX_SCALE,
                            divisions = 6,
                            start = { Text("א", style = TextStyle(fontSize = 14.sp, color = p.muted)) },
                            end = { Text("א", style = TextStyle(fontSize = 24.sp, color = p.muted)) },
                            onChange = s::setTextScale,
                        )
                        Spacer(Modifier.height(18.dp))
                        RowLabel(Icons.Rounded.DensityMedium, l.threadSpacing)
                        Spacer(Modifier.height(4.dp))
                        StepSlider(
                            value = s.threadDensity,
                            min = SettingsController.MIN_DENSITY,
                            max = SettingsController.MAX_DENSITY,
                            divisions = 8,
                            start = { Icon(Icons.Rounded.DensitySmall, null, tint = p.muted, modifier = Modifier.size(16.dp)) },
                            end = { Icon(Icons.Rounded.DensityLarge, null, tint = p.muted, modifier = Modifier.size(22.dp)) },
                            onChange = s::setThreadDensity,
                        )
                        // The back-gesture style.
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { s.setPredictiveBack(!s.predictiveBack) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowRightAlt, null, tint = p.muted)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(l.predictiveBack, style = TextStyle(fontSize = 16.sp, color = p.ink))
                                Text(l.predictiveBackHint, style = TextStyle(fontSize = 12.sp, color = p.muted))
                            }
                            Switch(
                                checked = s.predictiveBack,
                                onCheckedChange = s::setPredictiveBack,
                                colors = SwitchDefaults.colors(checkedTrackColor = accent),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card {
                    Column(Modifier.padding(16.dp)) {
                        RowLabel(Icons.Rounded.Translate, l.language)
                        Spacer(Modifier.height(12.dp))
                        Segmented(
                            listOf(
                                Triple(Icons.Rounded.Abc, l.hebrew, s.locale == "he"),
                                Triple(Icons.Rounded.Abc, l.english, s.locale == "en"),
                                Triple(Icons.Rounded.PhoneIphone, l.languageSystem, s.locale == null),
                            ),
                        ) { i -> s.setLocale(listOf("he", "en", null)[i]) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card {
                    ListItem(
                        modifier = Modifier.clickable { nav.push(AISettingsRoute()) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Rounded.AutoAwesome, null, tint = accent) },
                        headlineContent = {
                            Text(l.aiSection, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink, fontSize = 16.sp))
                        },
                        supportingContent = {
                            Text(if (AIStore.isReady) l.aiStatusConfigured else l.aiStatusOff, style = TextStyle(color = p.muted))
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = p.muted) },
                    )
                }
                Spacer(Modifier.height(28.dp))
                SectionLabel(l.account)
                Spacer(Modifier.height(10.dp))
                Card {
                    if (AuthService.loggedIn) {
                        ListItem(
                            modifier = Modifier.clickable { confirmSignOut = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = accent) },
                            headlineContent = {
                                Text(l.signOut, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink, fontSize = 16.sp))
                            },
                            supportingContent = { Text(AuthService.username ?: l.signedIn, style = TextStyle(color = p.muted)) },
                        )
                    } else {
                        ListItem(
                            modifier = Modifier.clickable { nav.home.scope.launch { openLogin(nav) } },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Icon(Icons.AutoMirrored.Rounded.Login, null, tint = accent) },
                            headlineContent = {
                                Text(l.signIn, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink, fontSize = 16.sp))
                            },
                            supportingContent = { Text(l.loginSubtitle, style = TextStyle(color = p.muted)) },
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                SectionLabel(l.about)
                Spacer(Modifier.height(10.dp))
                Card {
                    Text(l.aboutBody, style = TextStyle(color = p.muted, lineHeight = 1.5.em), modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp, color = palette.muted),
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun RowLabel(icon: ImageVector, text: String) {
    val p = palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = p.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.W700, color = p.ink))
    }
}

@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .dropShadowCompat(shape, Color.Black.copy(alpha = 0.04f), 14.dp, 5.dp)
            .clip(shape)
            .background(palette.surface),
        content = content,
    )
}

/** A segmented picker: [options] are (icon, label, selected). */
@Composable
private fun Segmented(options: List<Triple<ImageVector, String, Boolean>>, onSelect: (Int) -> Unit) {
    val p = palette
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.field, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, (icon, label, selected) ->
            val bg by animateColorAsState(if (selected) p.surface else Color.Transparent, tween(180), label = "seg")
            val shape = RoundedCornerShape(11.dp)
            Column(
                Modifier
                    .weight(1f)
                    .then(if (selected) Modifier.dropShadowCompat(shape, Color.Black.copy(alpha = 0.08f), 8.dp, 2.dp) else Modifier)
                    .background(bg, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (selected) return@clickable
                        view.selectionClick()
                        onSelect(i)
                    }
                    .padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null, tint = if (selected) accent else p.muted, modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(5.dp))
                Text(
                    label,
                    style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = if (selected) p.ink else p.muted),
                )
            }
        }
    }
}

@Composable
private fun StepSlider(
    value: Double,
    min: Double,
    max: Double,
    divisions: Int,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    onChange: (Double) -> Unit,
) {
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        start()
        Slider(
            value = value.toFloat(),
            onValueChange = { v ->
                val step = (max - min) / divisions
                val snapped = min + ((v - min) / step).roundToInt() * step
                if (kotlin.math.abs(snapped - value) > 1e-6) {
                    view.selectionClick()
                    onChange(snapped)
                }
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = divisions - 1,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        end()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(selected: Accent) {
    val p = palette
    val view = LocalView.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (a in Accent.entries) {
            val c = Color(a.seed)
            Box(
                Modifier
                    .size(42.dp)
                    .dropShadowCompat(CircleShape, c.copy(alpha = 0.4f), 8.dp, 3.dp)
                    .background(c, CircleShape)
                    .border(3.dp, if (a == selected) p.ink else Color.Transparent, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        view.selectionClick()
                        SettingsController.setAccent(a)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (a == selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
