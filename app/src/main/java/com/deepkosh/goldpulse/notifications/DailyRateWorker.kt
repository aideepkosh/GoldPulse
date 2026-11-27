package com.deepkosh.goldpulse.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepkosh.goldpulse.GoldPulseApp
import com.deepkosh.goldpulse.config.GoldConfig
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/** Fetches today's rate, posts the morning notification, then queues tomorrow's run. */
class DailyRateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GoldPulseApp

        val rates = try {
            app.repository.refresh()
        } catch (_: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS - 1) return Result.retry()
            null // Give up for today rather than notify with a stale rate.
        }

        if (rates != null) {
            val today = LocalDate.now(ZoneId.of(GoldConfig.RATE_TIME_ZONE)).toString()
            if (app.settings.lastNotifiedDay != today) {
                RateNotifier.showDailyRate(applicationContext, rates)
                app.settings.lastNotifiedDay = today
            }
        }

        app.scheduler.scheduleFollowingDay()
        return Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 4
    }
}
