package com.deepkosh.goldpulse.data

import android.content.Context
import androidx.core.content.edit

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var dailyNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_NOTIFICATION, true)
        set(value) = prefs.edit { putBoolean(KEY_DAILY_NOTIFICATION, value) }

    /** Whether the app has already asked for POST_NOTIFICATIONS on first launch. */
    var notificationPermissionAsked: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_PERMISSION_ASKED, value) }

    /** ISO date of the last daily notification, so a day never gets two. */
    var lastNotifiedDay: String?
        get() = prefs.getString(KEY_LAST_NOTIFIED_DAY, null)
        set(value) = prefs.edit { putString(KEY_LAST_NOTIFIED_DAY, value) }

    private companion object {
        const val KEY_DAILY_NOTIFICATION = "daily_notification_enabled"
        const val KEY_PERMISSION_ASKED = "notification_permission_asked"
        const val KEY_LAST_NOTIFIED_DAY = "last_notified_day"
    }
}
