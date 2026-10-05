package com.bennybarak.scoops.rotter_scoops

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.RotterLogin
import com.bennybarak.scoops.rotter_scoops.ui.AISettingsRoute
import com.bennybarak.scoops.rotter_scoops.ui.ComposeRoute
import com.bennybarak.scoops.rotter_scoops.ui.HomeRoute
import com.bennybarak.scoops.rotter_scoops.ui.LocalNav
import com.bennybarak.scoops.rotter_scoops.ui.LoginRoute
import com.bennybarak.scoops.rotter_scoops.ui.Navigator
import com.bennybarak.scoops.rotter_scoops.ui.ProfileRoute
import com.bennybarak.scoops.rotter_scoops.ui.Route
import com.bennybarak.scoops.rotter_scoops.ui.ScoopsTheme
import com.bennybarak.scoops.rotter_scoops.ui.Snacks
import com.bennybarak.scoops.rotter_scoops.ui.SummaryRoute
import com.bennybarak.scoops.rotter_scoops.ui.ThreadRoute
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.screens.AISettingsScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.ComposeScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.HomeScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.LoginScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.ProfileScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.SummaryScreen
import com.bennybarak.scoops.rotter_scoops.ui.screens.TabBarHeight
import com.bennybarak.scoops.rotter_scoops.ui.screens.ThreadScreen
import kotlinx.coroutines.launch

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

class MainActivity : ComponentActivity() {
    private val nav = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(nav) }
        // Refresh the rotter session in the background from saved credentials so
        // the user stays signed in across launches (no blocking, no prompt).
        // After the first frame: it spins up the hidden WebView.
        if (!sessionRefreshed) {
            sessionRefreshed = true
            window.decorView.post { lifecycleScope.launch { RotterLogin.refreshSession() } }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // A recreated activity builds its own navigator, so this one's screens
        // end here either way (most configuration changes are handled in place).
        nav.dispose()
    }

    override fun onStop() {
        super.onStop()
        nav.home.state.scoops.onPaused()
    }

    override fun onStart() {
        super.onStart()
        nav.home.state.scoops.onResumed()
    }

    companion object {
        private var sessionRefreshed = false
    }
}

@androidx.compose.runtime.Composable
private fun App(nav: Navigator) {
    ScoopsTheme {
        val p = palette
        val view = LocalView.current
        SideEffect {
            val c = WindowCompat.getInsetsController((view.context as ComponentActivity).window, view)
            c.isAppearanceLightStatusBars = !p.dark
            c.isAppearanceLightNavigationBars = !p.dark
        }
        CompositionLocalProvider(LocalNav provides nav) {
            Box(Modifier.fillMaxSize()) {
                val predictive = SettingsController.predictiveBack
                NavDisplay(
                    backStack = nav.backStack,
                    onBack = { nav.pop() },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                    transitionSpec = { if (predictive) fadeForward() else classicPush() },
                    popTransitionSpec = { if (predictive) fadeBackward() else classicPop() },
                    predictivePopTransitionSpec = { predictivePeek() },
                    entryProvider = { route -> NavEntry(route) { Screen(route) } },
                )
                // The classic style has no back-gesture peek: take the gesture
                // here, so it simply pops (with the classic animation) on release.
                if (!predictive && nav.backStack.size > 1) BackHandler { nav.pop() }
                SnackbarHost(
                    Snacks.host,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = if (nav.backStack.size == 1) TabBarHeight else 0.dp)
                        .padding(4.dp),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Screen(route: Route) {
    when (route) {
        is HomeRoute -> HomeScreen(route)
        is ThreadRoute -> ThreadScreen(route.state)
        is ProfileRoute -> ProfileScreen(route)
        is ComposeRoute -> ComposeScreen(route.state)
        is LoginRoute -> LoginScreen(route.state)
        is SummaryRoute -> SummaryScreen(route.state)
        is AISettingsRoute -> AISettingsScreen()
    }
}

// Android's "fade forwards" page motion (what Flutter's predictive-back builder
// uses for ordinary pushes/pops): 450ms, a quarter-width slide with a fade.
private fun AnimatedContentTransitionScope<Scene<Route>>.fadeForward(): ContentTransform =
    (slideInHorizontally(tween(450, easing = Emphasized)) { it / 4 } + fadeIn(tween(337))) togetherWith
        (slideOutHorizontally(tween(450, easing = Emphasized)) { -it / 4 } + fadeOut(tween(112)))

private fun AnimatedContentTransitionScope<Scene<Route>>.fadeBackward(): ContentTransform =
    ContentTransform(
        slideInHorizontally(tween(450, easing = Emphasized)) { -it / 4 } + fadeIn(tween(337)),
        slideOutHorizontally(tween(450, easing = Emphasized)) { it / 4 } + fadeOut(tween(112)),
        targetContentZIndex = -1f,
    )

/** The back-gesture peek: the page shrinks and fades, the previous one shows behind it. */
private fun AnimatedContentTransitionScope<Scene<Route>>.predictivePeek(): ContentTransform =
    ContentTransform(
        EnterTransition.None,
        scaleOut(targetScale = 0.9f) + fadeOut(),
        targetContentZIndex = -1f,
    )

// The classic style: the new page fades in while sliding up a little; the page
// underneath stays put.
private fun AnimatedContentTransitionScope<Scene<Route>>.classicPush(): ContentTransform =
    (fadeIn(tween(320, easing = EaseOutCubic)) + slideInVertically(tween(320, easing = EaseOutCubic)) { (it * 0.045f).toInt() }) togetherWith
        ExitTransition.KeepUntilTransitionsFinished

private fun AnimatedContentTransitionScope<Scene<Route>>.classicPop(): ContentTransform =
    ContentTransform(
        EnterTransition.None,
        fadeOut(tween(240, easing = EaseOutCubic)) + slideOutVertically(tween(240, easing = EaseOutCubic)) { (it * 0.045f).toInt() },
        targetContentZIndex = -1f,
    )
