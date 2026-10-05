package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.SummaryLanguage
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BackButton
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import kotlinx.coroutines.launch

/**
 * Configuration for the optional thread summary, on its own screen — an API
 * key, a model id and an endpoint are technical fields that made the main
 * Settings read as a developer page. Settings keeps a one-line status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen() {
    val l = strings
    val p = palette
    val ai = AIStore
    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    // Held only until written to secure storage, then cleared.
    var key by remember { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf(ai.model) }
    var endpoint by rememberSaveable { mutableStateOf(ai.baseUrl) }
    var advanced by rememberSaveable { mutableStateOf(false) }

    fun saveKey() {
        if (key.isBlank()) return
        val k = key
        scope.launch {
            ai.setKey(k)
            key = ""
            focus.clearFocus()
        }
    }

    @Composable
    fun Section(content: @Composable () -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(p.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) { content() }
    }

    val ltr = TextStyle(textDirection = TextDirection.Ltr, fontSize = 16.sp, color = p.ink)

    AppScaffold(bar = { AppBar(leading = { BackButton() }, title = { BarTitle(l.aiSection) }) }) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        ) {
            item {
                Section {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            l.aiEnable,
                            style = TextStyle(fontWeight = FontWeight.W700, color = p.ink, fontSize = 16.sp),
                            modifier = Modifier.weight(1f),
                        )
                        // Nothing to call with until a key is stored.
                        Switch(
                            checked = ai.enabled,
                            onCheckedChange = ai::setEnabled,
                            enabled = ai.hasKey,
                            colors = SwitchDefaults.colors(checkedTrackColor = accent),
                        )
                    }
                    // Stated plainly: this ships the thread's text off the device
                    // to a third party, which nothing else in the app does.
                    Text(l.aiPrivacyNote, style = TextStyle(fontSize = 12.5.sp, lineHeight = 1.45.em, color = p.muted))
                }
                Spacer(Modifier.height(14.dp))
                Section {
                    Text(l.aiKey, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = ltr,
                        placeholder = { Text(if (ai.hasKey) l.aiKeyStored else l.aiKeyPlaceholder) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { saveKey() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { saveKey() }, enabled = key.isNotBlank()) { Text(l.aiSaveKey) }
                        Spacer(Modifier.weight(1f))
                        if (ai.hasKey) {
                            TextButton(
                                onClick = {
                                    key = ""
                                    scope.launch { ai.clearKey() }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                            ) { Text(l.aiRemoveKey) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Section {
                    Text(l.aiLanguage, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink))
                    Spacer(Modifier.height(10.dp))
                    val opts = listOf(
                        SummaryLanguage.followApp to l.aiLanguageFollowApp,
                        SummaryLanguage.hebrew to l.aiLanguageHebrew,
                        SummaryLanguage.english to l.aiLanguageEnglish,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opts.forEachIndexed { i, (v, label) ->
                            SegmentedButton(
                                selected = ai.language == v,
                                onClick = { ai.setLanguage(v) },
                                shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                                icon = {},
                            ) { Text(label, maxLines = 1) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Section {
                    Row(
                        Modifier.fillMaxWidth().clickable { advanced = !advanced },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(l.aiAdvanced, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink), modifier = Modifier.weight(1f))
                        Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = p.muted)
                    }
                    if (advanced) {
                        Spacer(Modifier.height(12.dp))
                        // Editable: model ids change often, and a wrong one should
                        // be a line of text to fix, not a rebuild.
                        OutlinedTextField(
                            value = model,
                            onValueChange = {
                                model = it
                                ai.setModel(it)
                            },
                            singleLine = true,
                            textStyle = ltr,
                            label = { Text(l.aiModel) },
                            placeholder = { Text(AIStore.DEFAULT_MODEL) },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = {
                                endpoint = it
                                ai.setBaseUrl(it)
                            },
                            singleLine = true,
                            textStyle = ltr,
                            label = { Text(l.aiEndpoint) },
                            placeholder = { Text(AIStore.DEFAULT_BASE_URL) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
