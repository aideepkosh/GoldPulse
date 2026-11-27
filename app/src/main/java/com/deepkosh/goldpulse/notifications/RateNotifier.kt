package com.deepkosh.goldpulse.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.deepkosh.goldpulse.MainActivity
import com.deepkosh.goldpulse.R
import com.deepkosh.goldpulse.data.GoldRates
import com.deepkosh.goldpulse.data.Karat
import com.deepkosh.goldpulse.ui.RateFormat
import kotlin.math.abs

object RateNotifier {

    private const val CHANNEL_ID = "daily_gold_rate"
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(context.getString(R.string.notification_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** POST_NOTIFICATIONS is a runtime permission on Android 13+ and granted at install below that. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** True if the app may post notifications: permission granted and not blocked in system settings. */
    fun canNotify(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun showDailyRate(context: Context, rates: GoldRates) {
        if (!canNotify(context)) return

        val k24 = rates.rate(Karat.K24)
        val k22 = rates.rate(Karat.K22)

        val title = context.getString(
            R.string.notification_title,
            RateFormat.shortDate(rates.live.fetchedAtMillis),
        )
        val text = context.getString(
            R.string.notification_text,
            RateFormat.rupees(k24.perGram),
            RateFormat.rupees(k22.perGram),
        )
        val details = buildList {
            add(
                context.getString(
                    R.string.notification_rates,
                    RateFormat.rupees(k24.perGram),
                    RateFormat.rupees(k24.perTenGrams),
                    RateFormat.rupees(k22.perGram),
                    RateFormat.rupees(k22.perTenGrams),
                ),
            )
            val change = k24.changePerTenGrams
            val percent = k24.changePercent
            if (change != null && percent != null) {
                val changeText = context.getString(
                    RateFormat.changeTemplate(RateFormat.trend(change)),
                    RateFormat.rupees(abs(change)),
                    RateFormat.percent(abs(percent) / 100),
                )
                add(context.getString(R.string.notification_change, changeText))
            }
            add(context.getString(R.string.notification_disclaimer))
        }.joinToString("\n")

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_gold)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; nothing to do.
        }
    }
}
