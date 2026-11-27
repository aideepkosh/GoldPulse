package com.deepkosh.goldpulse.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.deepkosh.goldpulse.config.GoldConfig
import com.deepkosh.goldpulse.data.AppSettings
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily rate notification as a chain of one-time jobs, each aimed at the next
 * NOTIFICATION_HOUR:NOTIFICATION_MINUTE. Unlike periodic work, this does not drift over time.
 */
class DailyRateScheduler(
    private val context: Context,
    private val settings: AppSettings,
) {
    val isEnabled: Boolean get() = settings.dailyNotificationEnabled

    fun setEnabled(enabled: Boolean) {
        settings.dailyNotificationEnabled = enabled
        if (enabled) {
            ensureScheduled()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    /** Schedules the next run unless one is already pending. Safe to call on every app start. */
    fun ensureScheduled() {
        if (!isEnabled) return
        enqueue(ExistingWorkPolicy.KEEP, delayUntilNextRun(ZonedDateTime.now()))
    }

    /** Called by the worker when it finishes, to queue tomorrow's run after itself. */
    fun scheduleFollowingDay() {
        if (!isEnabled) return
        // Look at least an hour ahead so a run that fires slightly early can't re-queue for today.
        val now = ZonedDateTime.now()
        enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE, delayUntilNextRun(now, notBefore = now.plusHours(1)))
    }

    private fun enqueue(policy: ExistingWorkPolicy, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<DailyRateWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    companion object {
        private const val WORK_NAME = "daily_gold_rate"

        private val runTime: LocalTime =
            LocalTime.of(GoldConfig.NOTIFICATION_HOUR, GoldConfig.NOTIFICATION_MINUTE)

        /** Milliseconds from [now] until the first notification time strictly after [notBefore]. */
        fun delayUntilNextRun(now: ZonedDateTime, notBefore: ZonedDateTime = now): Long {
            var next = notBefore.with(runTime)
            if (!next.isAfter(notBefore)) next = next.plusDays(1)
            return Duration.between(now, next).toMillis()
        }
    }
}
