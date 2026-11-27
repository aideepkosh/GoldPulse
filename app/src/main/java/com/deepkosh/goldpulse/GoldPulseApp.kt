package com.deepkosh.goldpulse

import android.app.Application
import com.deepkosh.goldpulse.data.AppSettings
import com.deepkosh.goldpulse.data.GoldRateRepository
import com.deepkosh.goldpulse.data.RateCache
import com.deepkosh.goldpulse.data.RateSources
import com.deepkosh.goldpulse.notifications.DailyRateScheduler
import com.deepkosh.goldpulse.notifications.RateNotifier

class GoldPulseApp : Application() {

    val settings: AppSettings by lazy { AppSettings(this) }
    val repository: GoldRateRepository by lazy { GoldRateRepository(RateSources(), RateCache(this)) }
    val scheduler: DailyRateScheduler by lazy { DailyRateScheduler(this, settings) }

    override fun onCreate() {
        super.onCreate()
        RateNotifier.createChannel(this)
        scheduler.ensureScheduled()
    }
}
