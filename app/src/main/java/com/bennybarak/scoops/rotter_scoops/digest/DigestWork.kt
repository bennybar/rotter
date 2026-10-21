package com.bennybarak.scoops.rotter_scoops.digest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bennybarak.scoops.rotter_scoops.MainActivity
import com.bennybarak.scoops.rotter_scoops.R
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.DiskCache
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.Scoop
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.ui.currentStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Runs a digest in the background ([DigestEngine.shared]) with an ongoing
 * notification, so it finishes when the user leaves or closes the app. Used
 * for "Summarize" taps and for the automatic schedule. Posts "Digest ready"
 * when a new digest was made and the app isn't on screen.
 */
class DigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo =
        DigestNotifications.progressInfo(applicationContext, null, false)

    override suspend fun doWork(): Result {
        val window = inputData.getInt(DigestScheduler.KEY_WINDOW, 60)
        val auto = inputData.getBoolean(DigestScheduler.KEY_AUTO, false)
        // A schedule left on after summaries were turned off or the key removed.
        if (!AIStore.enabled || AIStore.apiKey() == null) return Result.success()
        try {
            setForeground(DigestNotifications.progressInfo(applicationContext, null, false))
        } catch (_: Exception) {
            // Not allowed from the background on this device right now: run
            // without the notification (the job still completes).
        }
        val engine = DigestEngine.shared
        val made = withContext(Dispatchers.Main) {
            if (!engine.tryStart()) return@withContext null // a run is already going
            // Keep the notification's progress in step with the engine.
            val updates = launch {
                snapshotFlow { engine.progress to engine.summarizing }.collect { (pr, summarizing) ->
                    runCatching { setForeground(DigestNotifications.progressInfo(applicationContext, pr, summarizing)) }
                }
            }
            try {
                engine.run(window, currentStrings()) { latestFeed() }
            } finally {
                updates.cancel()
            }
        }
        val visible = withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        if (made != null && (auto || !visible)) DigestNotifications.ready(applicationContext, made)
        return Result.success()
    }

    /** The feed, fetched now; the list's last saved copy if rotter doesn't answer. */
    private suspend fun latestFeed(): List<Scoop> =
        withTimeoutOrNull(10_000) { runCatching { RotterService.fetchScoops() }.getOrNull() }
            ?: (DiskCache.read("scoops.json") as? JSONArray)?.let { a ->
                (0 until a.length()).mapNotNull { runCatching { Scoop.fromJson(a.getJSONObject(it)) }.getOrNull() }
            }
            ?: emptyList()
}

/** Starting digest jobs: one now, or on a schedule. */
object DigestScheduler {
    const val KEY_WINDOW = "window"
    const val KEY_AUTO = "auto"
    private const val NOW = "digest-now"
    private const val AUTO = "digest-auto"
    private const val AUTO_KEY = "digest_auto_minutes"

    /** The automatic-digest choices, in minutes (0 = off). */
    val AUTO_CHOICES = listOf(0, 15, 30, 60, 120, 480)

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** A digest of the last [window] minutes, now, in the background. */
    fun runNow(context: Context, window: Int) {
        val req = OneTimeWorkRequestBuilder<DigestWorker>()
            .setInputData(workDataOf(KEY_WINDOW to window, KEY_AUTO to false))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(online)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, req)
    }

    var autoMinutes: Int
        get() = Prefs.getString(AUTO_KEY)?.toIntOrNull() ?: 0
        private set(v) = Prefs.setString(AUTO_KEY, v.toString())

    /** Turn the schedule on (every [minutes]) or off (0). */
    fun setAuto(context: Context, minutes: Int) {
        autoMinutes = minutes
        schedule(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    /** When the next automatic digest is due (epoch ms); null while it's off. */
    fun nextAuto(context: Context): Flow<Long?> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(AUTO).map { infos ->
            infos.firstOrNull { !it.state.isFinished }?.nextScheduleTimeMillis?.takeIf { it != Long.MAX_VALUE }
        }

    /** At app start: make sure the saved schedule is in place. */
    fun ensure(context: Context) = schedule(context, ExistingPeriodicWorkPolicy.KEEP)

    private fun schedule(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val wm = WorkManager.getInstance(context)
        val minutes = autoMinutes
        if (minutes <= 0) {
            wm.cancelUniqueWork(AUTO)
            return
        }
        // Each run looks back a little further than the interval, so a late
        // run leaves no gap; scoops already in a kept digest aren't repeated.
        val window = minOf(minutes + 15, 24 * 60)
        val req = PeriodicWorkRequestBuilder<DigestWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_WINDOW to window, KEY_AUTO to true))
            .setConstraints(online)
            .build()
        wm.enqueueUniquePeriodicWork(AUTO, policy, req)
    }
}

/** The digest job's notifications: ongoing progress, and "ready". */
object DigestNotifications {
    private const val PROGRESS = "digest_progress"
    private const val READY = "digest_ready"
    private const val PROGRESS_ID = 4001
    private const val READY_ID = 4002

    private fun channels(context: Context) {
        val l = currentStrings()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(PROGRESS, l.digestNotifyProgressChannel, NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(READY, l.digestNotifyReadyChannel, NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun progressInfo(context: Context, progress: Pair<Int, Int>?, summarizing: Boolean): ForegroundInfo {
        channels(context)
        val l = currentStrings()
        val b = NotificationCompat.Builder(context, PROGRESS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(l.digestTitle)
            .setContentText(
                when {
                    summarizing -> l.aiSummarizing
                    progress != null -> l.digestLoading(progress.first, progress.second)
                    else -> l.digestUpdating
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context))
        if (summarizing || progress == null) {
            b.setProgress(0, 0, true)
        } else {
            b.setProgress(progress.second.coerceAtLeast(1), progress.first, false)
        }
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(PROGRESS_ID, b.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(PROGRESS_ID, b.build())
        }
    }

    fun ready(context: Context, d: KeptDigest) {
        channels(context)
        val l = currentStrings()
        val first = d.text.lines().map { it.trim() }.firstOrNull { it.startsWith("-") || it.startsWith("•") }
            ?.trimStart('-', '•', ' ')
            ?.replace(Regex("""\*\*|\s*\[[\d,\s]+\]"""), "")
        val n = NotificationCompat.Builder(context, READY)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(l.digestNotifyReady(d.ids.size))
            .setContentText(first ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(first ?: ""))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(READY_ID, n)
        } catch (_: SecurityException) { /* notifications not allowed */ }
    }
}
