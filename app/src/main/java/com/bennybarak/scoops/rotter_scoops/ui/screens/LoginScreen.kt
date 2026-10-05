package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.net.LoginOutcome
import com.bennybarak.scoops.rotter_scoops.net.RotterLogin
import com.bennybarak.scoops.rotter_scoops.ui.Danger
import com.bennybarak.scoops.rotter_scoops.ui.Route
import com.bennybarak.scoops.rotter_scoops.ui.Strings
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BackButton
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import kotlinx.coroutines.launch

/**
 * Our own sign-in screen. It collects the rotter username + password and signs
 * in through the hidden WebView (so Cloudflare just works), saving the
 * credentials in the Keystore so future sessions are silent. On failure it
 * shows an inline error — there is no website fallback. Finishes with true
 * when signed in.
 */
class LoginState(private val route: Route) {
    var user by mutableStateOf(TextFieldValue())
    var pass by mutableStateOf(TextFieldValue())
    var busy by mutableStateOf(false); private set
    var autoTrying by mutableStateOf(false); private set // signing in with saved creds first
    var error by mutableStateOf<String?>(null); private set

    /** Set by the screen each composition. */
    var l: Strings? = null

    init {
        route.scope.launch { maybeAutoLogin() }
    }

    private suspend fun maybeAutoLogin() {
        val creds = AuthService.credentials() ?: return
        user = TextFieldValue(creds.user)
        pass = TextFieldValue(creds.pass)
        autoTrying = true
        val r = RotterLogin.attempt(creds.user, creds.pass)
        if (r.outcome == LoginOutcome.success) {
            route.finish(true)
        } else {
            // Saved creds didn't work silently — reveal the form (with an error
            // only if they were actually rejected).
            autoTrying = false
            error = if (r.outcome == LoginOutcome.wrongCredentials) l?.loginFailed else null
        }
    }

    fun submit(clearFocus: () -> Unit) {
        val u = user.text.trim()
        val p = pass.text
        if (u.isEmpty() || p.isEmpty()) return
        clearFocus()
        busy = true
        error = null
        route.scope.launch {
            val r = RotterLogin.attempt(u, p)
            when (r.outcome) {
                LoginOutcome.success -> {
                    AuthService.saveCredentials(u, p)
                    route.finish(true)
                }
                LoginOutcome.wrongCredentials -> {
                    busy = false
                    error = l?.loginFailed
                }
                LoginOutcome.failed -> {
                    busy = false
                    error = l?.loginError
                }
            }
        }
    }
}

@Composable
fun LoginScreen(s: LoginState) {
    val l = strings
    val p = palette
    s.l = l
    val focusManager = LocalFocusManager.current
    val accent = MaterialTheme.colorScheme.primary
    AppScaffold(bar = { AppBar(leading = { BackButton() }, title = { BarTitle(l.signIn) }) }) {
        if (s.autoTrying) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@AppScaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
        ) {
            item {
                Text(l.loginSubtitle, style = TextStyle(color = p.muted, lineHeight = 1.5.em, fontSize = 14.5.sp))
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(
                    value = s.user,
                    onValueChange = { s.user = it },
                    label = { Text(l.usernameLabel) },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = s.pass,
                    onValueChange = { s.pass = it },
                    label = { Text(l.passwordLabel) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!s.busy) s.submit { focusManager.clearFocus() } }),
                    modifier = Modifier.fillMaxWidth(),
                )
                val err = s.error
                if (err != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = Danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(err, style = TextStyle(color = Danger, fontWeight = FontWeight.W600))
                    }
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { s.submit { focusManager.clearFocus() } },
                    enabled = !s.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    if (s.busy) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.4.dp, color = Color.White)
                    } else {
                        Text(l.signIn, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700))
                    }
                }
            }
        }
    }
}
