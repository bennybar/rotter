package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.ui.HomeRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.openLogin
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.selectionClick
import kotlinx.coroutines.launch

/** Height of the bottom tab bar (Material 3 NavigationBar), above the system inset. */
val TabBarHeight = 80.dp

/** The home screen's state: the selected tab and each tab's own state, for the session. */
class HomeState(route: HomeRoute) {
    var index by mutableIntStateOf(0)
    val scoops = ScoopsController(route.scope)
    val compose = ComposeState(route, threadId = null, parentNum = 0, editNum = null, embedded = true)
}

/**
 * Root section navigation: Scoops / New message / Settings over a translucent
 * Material 3 NavigationBar; the lists run full-screen behind it.
 */
@Composable
fun HomeScreen(route: HomeRoute) {
    val s = route.state
    val l = strings
    val p = palette
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary
    val bottomInset = TabBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Back from another tab returns to Scoops first, rather than leaving the app.
    BackHandler(enabled = s.index != 0) { s.index = 0 }

    Box(Modifier.fillMaxSize()) {
        when (s.index) {
            0 -> ScoopsScreen(s.scoops, bottomInset)
            1 -> NewMessageScreen(s.compose, bottomInset)
            else -> SettingsScreen(bottomInset)
        }
        val items = listOf(
            Triple(Icons.Outlined.Bolt, Icons.Rounded.Bolt, l.tabScoops),
            Triple(Icons.Outlined.Edit, Icons.Rounded.Edit, l.tabNewMessage),
            Triple(Icons.Outlined.Settings, Icons.Rounded.Settings, l.tabSettings),
        )
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            // Flutter blurred what showed through; without a backdrop blur it stays
            // more opaque so the list behind doesn't compete with the labels.
            containerColor = p.surface.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
        ) {
            items.forEachIndexed { i, (off, on, label) ->
                NavigationBarItem(
                    selected = s.index == i,
                    onClick = {
                        if (i != s.index) {
                            view.selectionClick()
                            s.index = i
                        }
                    },
                    icon = { Icon(if (s.index == i) on else off, null, tint = if (s.index == i) accent else LocalContentColor.current) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(),
                )
            }
        }
    }
}

/**
 * The "new message" tab. Composing a new scoop thread requires a signed-in
 * account, so this either prompts to sign in or shows the composer itself.
 */
@Composable
private fun NewMessageScreen(compose: ComposeState, bottomInset: Dp) {
    if (AuthService.loggedIn) {
        ComposeScreen(compose, bottomInset)
        return
    }
    val l = strings
    val p = palette
    val nav = LocalNav.current
    AppScaffold(bar = { AppBar(title = { BarTitle(l.tabNewMessage) }) }) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(20.dp))
                Text(l.newMessagePrompt, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    l.newMessagePromptBody,
                    style = TextStyle(color = p.muted, lineHeight = 1.5.em, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { nav.home.scope.launch { openLogin(nav) } },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, null)
                    Spacer(Modifier.size(8.dp))
                    Text(l.signIn)
                }
            }
        }
    }
}
