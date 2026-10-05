package com.bennybarak.scoops.rotter_scoops.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App-wide floating snackbars (Flutter's ScaffoldMessenger): one host at the
 * root, reachable from screen logic that outlives a composition.
 */
object Snacks {
    val host = SnackbarHostState()
    private val scope = MainScope()
    private var job: Job? = null

    /** Show [text] for [ms] milliseconds (Flutter's default is 4s), replacing any current one. */
    fun show(text: String, ms: Long = 4000) {
        job?.cancel()
        job = scope.launch {
            host.currentSnackbarData?.dismiss()
            val shown = launch { host.showSnackbar(text, duration = SnackbarDuration.Indefinite) }
            delay(ms)
            shown.cancel()
        }
    }
}
