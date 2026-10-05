package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.data.DraftStore
import com.bennybarak.scoops.rotter_scoops.data.MyRepliesStore
import com.bennybarak.scoops.rotter_scoops.net.PostOutcome
import com.bennybarak.scoops.rotter_scoops.net.RotterPost
import com.bennybarak.scoops.rotter_scoops.ui.ComposeRoute
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import com.bennybarak.scoops.rotter_scoops.ui.Navigator
import com.bennybarak.scoops.rotter_scoops.ui.NotoSansHebrew
import com.bennybarak.scoops.rotter_scoops.ui.Route
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.StringsHe
import com.bennybarak.scoops.rotter_scoops.ui.openLogin
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.mediumImpact
import kotlinx.coroutines.launch

/**
 * Open the composer, signing in first when needed. Reply controls stay enabled
 * while signed out — an inert button reads as broken rather than gated — so
 * this signs in and then opens the composer that was actually asked for.
 * Returns the composer's result (true = posted), or null if sign-in was abandoned.
 */
suspend fun openComposer(nav: Navigator, route: ComposeRoute): Boolean? {
    if (!AuthService.loggedIn) {
        if (!openLogin(nav)) return null
    }
    return nav.pushForResult<Boolean>(route)
}

/**
 * Native composer for a reply, a new thread, or editing your own message.
 * Submits directly to rotter (no webview UI). On failure it shows an error —
 * there is no website fallback. Finishes with `true` when posted/saved.
 *
 * [embedded] is the New-message tab's composer: nothing to pop, so a
 * successful post clears the fields instead.
 */
class ComposeState(
    private val route: Route,
    val threadId: String?,
    val parentNum: Int,
    val editNum: Int?,
    val embedded: Boolean,
) {
    val isEdit = editNum != null
    val isNewThread = threadId == null && !isEdit

    /** Which draft this composer reads and writes. */
    private val draftKey = when {
        isEdit -> "e:$threadId:$editNum"
        isNewThread -> "new"
        else -> "r:$threadId:$parentNum"
    }

    var subject by mutableStateOf(TextFieldValue()); private set
    var body by mutableStateOf(TextFieldValue()); private set
    var busy by mutableStateOf(false); private set
    var loadingDraft by mutableStateOf(false); private set
    var confirmDiscard by mutableStateOf(false)

    /** Set by the screen each composition. */
    var l: Strings? = null
    var haptic: () -> Unit = {}

    init {
        if (isEdit) loadDraft() else restoreDraft()
    }

    // Autosaved as it's typed, so a back-swipe or a failed send never costs
    // the user their text.
    fun onSubject(v: TextFieldValue) {
        subject = v
        saveDraft()
    }

    fun onBody(v: TextFieldValue) {
        body = v
        saveDraft()
    }

    private fun saveDraft() {
        if (loadingDraft) return
        DraftStore.save(draftKey, subject.text, body.text)
    }

    private fun restoreDraft() {
        val d = DraftStore.draft(draftKey) ?: return
        subject = TextFieldValue(d.first)
        body = TextFieldValue(d.second)
    }

    private val hasText get() = subject.text.isNotBlank() || body.text.isNotBlank()

    /** The explicit close offers to throw the text away; a back-swipe keeps it (it's autosaved). */
    fun close() {
        if (!hasText) {
            route.finish(false)
            return
        }
        confirmDiscard = true
    }

    fun discard() {
        confirmDiscard = false
        DraftStore.clear(draftKey)
        route.finish(false)
    }

    /** Pull the message's current text off rotter's edit form to pre-fill. */
    private fun loadDraft() {
        loadingDraft = true
        route.scope.launch {
            val draft = RotterPost.loadForEdit(threadId!!, editNum!!)
            if (draft != null) {
                subject = TextFieldValue(draft.subject)
                body = TextFieldValue(draft.body)
            }
            // A saved draft is newer than whatever the server had, so it wins.
            restoreDraft()
            loadingDraft = false
            if (draft == null) l?.let { Snacks.show(it.postFailed, 3000) }
        }
    }

    fun send(clearFocus: () -> Unit) {
        val l = l ?: return
        val bodyText = body.text.trim()
        val subjectText = subject.text.trim()
        // A title alone is enough — content isn't required if there's a title.
        if (subjectText.isEmpty() && bodyText.isEmpty()) return
        clearFocus()
        val nav = route.navigator
        route.scope.launch {
            if (!AuthService.loggedIn) {
                if (!openLogin(nav)) return@launch
            }
            busy = true
            suspend fun submit(): PostOutcome = when {
                isEdit -> RotterPost.edit(threadId!!, editNum!!, subjectText, bodyText)
                isNewThread -> RotterPost.newThread(subjectText, bodyText)
                else -> RotterPost.reply(threadId!!, parentNum, subjectText, bodyText)
            }

            var outcome = submit()
            if (outcome == PostOutcome.notLoggedIn) {
                if (openLogin(nav)) outcome = submit()
            }

            if (outcome == PostOutcome.success) {
                haptic()
                // Editing an existing message isn't a new reply of ours to remember.
                if (!isNewThread && !isEdit) MyRepliesStore.add(threadId!!)
                DraftStore.clear(draftKey)
                if (embedded) {
                    // A tab root has nowhere to go: clear the fields and stay put.
                    subject = TextFieldValue()
                    body = TextFieldValue()
                    busy = false
                    Snacks.show(l.postSuccess)
                } else {
                    route.finish(true)
                }
                return@launch
            }

            // Couldn't post — show the error and let them retry (no website
            // fallback); the text stays, and is in the draft too.
            val msg = when (outcome) {
                PostOutcome.notLoggedIn -> l.notSignedInError
                PostOutcome.blocked -> l.blockedError
                else -> l.postFailed
            }
            Snacks.show(msg, 3000)
            busy = false
        }
    }
}

@Composable
fun ComposeScreen(s: ComposeState, bottomInset: androidx.compose.ui.unit.Dp = 0.dp) {
    val l = strings
    val p = palette
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    s.l = l
    s.haptic = { view.mediumImpact() }
    val accent = MaterialTheme.colorScheme.primary
    val title = if (s.isEdit) l.edit else if (s.isNewThread) l.compose else l.reply

    // Leaving mid-send cancels our side but not the post itself — it would
    // land with its draft kept, inviting a duplicate. Hold back until it ends.
    BackHandler(enabled = s.busy) { }
    AppScaffold(
        bar = {
            AppBar(
                title = { BarTitle(title) },
                leading = if (s.embedded) null else ({
                    IconButton(onClick = { s.close() }, enabled = !s.busy) { Icon(Icons.Rounded.Close, l.cancel) }
                }),
                actions = {
                    Box(Modifier.padding(end = 4.dp)) {
                        TextButton(
                            onClick = { s.send { focusManager.clearFocus() } },
                            enabled = !s.busy && !s.loadingDraft,
                        ) {
                            if (s.busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.2.dp)
                            } else {
                                Text(
                                    if (s.isEdit) l.save else l.send,
                                    style = TextStyle(fontWeight = FontWeight.W800, fontSize = 15.5.sp, color = accent),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) {
        if (s.loadingDraft) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@AppScaffold
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime).union(WindowInsets(bottom = bottomInset)),
                    )
                    .padding(16.dp),
            ) {
                val subjectFocus = remember { FocusRequester() }
                // Focus the title on load, but not when editing (text is
                // prefilled) or as a tab root (the keyboard would pop over the list).
                if (!s.isEdit && !s.embedded) {
                    LaunchedEffect(Unit) { subjectFocus.requestFocus() }
                }
                val plain = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
                // The hints are always Hebrew: what is typed here is Hebrew content
                // posted to rotter, whatever the interface language.
                // Both replies and new threads carry a title on rotter (optional
                // on replies — empty keeps rotter's "Re:…" default).
                TextField(
                    value = s.subject,
                    onValueChange = s::onSubject,
                    enabled = !s.busy,
                    singleLine = true,
                    placeholder = { Text(StringsHe.subjectHint, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800, color = p.ink, fontFamily = NotoSansHebrew),
                    colors = plain,
                    modifier = Modifier.fillMaxWidth().focusRequester(subjectFocus),
                )
                HorizontalDivider(color = p.field, thickness = 1.dp)
                Spacer(Modifier.height(4.dp))
                TextField(
                    value = s.body,
                    onValueChange = s::onBody,
                    enabled = !s.busy,
                    placeholder = { Text(if (s.isNewThread) StringsHe.bodyHint else StringsHe.composeHint, style = TextStyle(fontSize = 16.sp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(fontSize = 16.sp, lineHeight = 1.5.em, color = p.ink, fontFamily = NotoSansHebrew),
                    colors = plain,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }

    if (s.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { s.confirmDiscard = false },
            text = { Text(l.discardDraftTitle) },
            dismissButton = { TextButton(onClick = { s.confirmDiscard = false }) { Text(l.keepEditing) } },
            confirmButton = {
                TextButton(
                    onClick = { s.discard() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                ) { Text(l.discard) }
            },
        )
    }
}
