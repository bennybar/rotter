package com.bennybarak.scoops.rotter_scoops.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.AndroidUiDispatcher
import com.bennybarak.scoops.rotter_scoops.data.Message
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.data.Thread
import com.bennybarak.scoops.rotter_scoops.ui.screens.ComposeState
import com.bennybarak.scoops.rotter_scoops.ui.screens.HomeState
import com.bennybarak.scoops.rotter_scoops.ui.screens.LoginState
import com.bennybarak.scoops.rotter_scoops.ui.screens.SummaryState
import com.bennybarak.scoops.rotter_scoops.ui.screens.ThreadState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A screen on the back stack. Unlike composition state, a route — and the
 * screen state it owns — lives exactly as long as it is on the stack, so a
 * screen covered by another one keeps its scroll position, loaded data and
 * in-flight work (a post waiting on sign-in), as Flutter's navigator did.
 * Work started in [scope] runs on the UI thread with a frame clock (for
 * animated scrolls) and is cancelled when the route is popped.
 */
sealed class Route {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)

    /** What a pushed screen reports back with (null when dismissed by back). */
    open val result: CompletableDeferred<Any?>? get() = null

    /** The navigator this route was pushed onto. */
    lateinit var navigator: Navigator

    /** Close this screen, reporting [result] to whoever opened it. */
    fun finish(result: Any?) = navigator.pop(this, result)

    open fun onPopped() {
        scope.cancel()
    }
}

class HomeRoute : Route() {
    val state = HomeState(this)
}

class ThreadRoute(val scoop: Scoop) : Route() {
    val state = ThreadState(scoop, scope)
}

class ProfileRoute(
    val name: String,
    val joinDate: String?,
    val messages: Int?,
    val raters: Int?,
    val points: Int?,
    val posts: List<Message>,
    val baseUrl: String,
) : Route()

/**
 * The composer for a reply, a new thread, or editing your own message.
 * [threadId] null (with no [editNum]) composes a NEW thread.
 */
class ComposeRoute(
    val threadId: String? = null,
    val parentNum: Int = 0, // 0 = reply to the original post
    val editNum: Int? = null, // edit this message of threadId instead (0 = the root)
) : Route() {
    override val result = CompletableDeferred<Any?>()
    val state = ComposeState(this, threadId, parentNum, editNum, embedded = false)
}

class LoginRoute : Route() {
    override val result = CompletableDeferred<Any?>()
    val state = LoginState(this)
}

class SummaryRoute(val thread: Thread, val title: String) : Route() {
    val state = SummaryState(thread, title, scope)
}

class AISettingsRoute : Route()

/**
 * The app's back stack. [pushForResult] awaits the pushed screen's result the
 * way a Flutter `Navigator.push` future does: whatever it [pop]s with, or null
 * when it is dismissed by back.
 */
class Navigator {
    val home = HomeRoute().also { it.navigator = this }
    val backStack = mutableStateListOf<Route>(home)

    fun push(route: Route) {
        route.navigator = this
        backStack.add(route)
    }

    suspend fun <T> pushForResult(route: Route): T? {
        push(route)
        @Suppress("UNCHECKED_CAST")
        return route.result?.await() as T?
    }

    /** Pop the top screen (back), reporting [result] to whoever pushed it. */
    fun pop(result: Any? = null) {
        if (backStack.size <= 1) return
        finish(backStack.removeAt(backStack.lastIndex), result)
    }

    /** Pop [route] specifically, wherever it is (a screen finishing itself). */
    fun pop(route: Route, result: Any?) {
        if (backStack.size > 1 && backStack.remove(route)) finish(route, result)
    }

    private fun finish(route: Route, result: Any?) {
        route.result?.complete(result)
        route.onPopped()
    }
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("no navigator") }

/** Open the sign-in screen; true if signed in. */
suspend fun openLogin(nav: Navigator): Boolean = nav.pushForResult<Boolean>(LoginRoute()) ?: false
