package com.deepkosh.goldpulse.ui

import android.icu.text.NumberFormat
import android.icu.util.ULocale
import androidx.annotation.StringRes
import com.deepkosh.goldpulse.R
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Number and date formatting shared by the screen and the notification. */
object RateFormat {

    private val india = ULocale("en_IN")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.US)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /** Whole rupees with Indian digit grouping, e.g. ₹1,23,456. */
    fun rupees(amount: Double): String = NumberFormat.getCurrencyInstance(india).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(amount.roundToLong())

    /** Rupees with paise, e.g. ₹92.52. */
    fun rupeesWithPaise(amount: Double): String = NumberFormat.getCurrencyInstance(india).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(amount)

    fun usd(amount: Double): String = NumberFormat.getCurrencyInstance(ULocale.US).format(amount)

    /** [fraction] 0.0123 -> "1.23%". */
    fun percent(fraction: Double, maxDecimals: Int = 2, minDecimals: Int = maxDecimals): String =
        NumberFormat.getPercentInstance(india).apply {
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = minDecimals
        }.format(fraction)

    fun dateTime(epochMillis: Long): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun shortDate(epochMillis: Long): String =
        shortDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    /** e.g. "9:00 AM", with a non-breaking space so it never wraps. */
    fun time(time: LocalTime): String = timeFormatter.format(time).replace(' ', ' ')

    enum class Trend { UP, DOWN, FLAT }

    /** Direction of a rupee change, treating anything under ₹1 as flat. */
    fun trend(change: Double): Trend = when {
        abs(change).roundToLong() == 0L -> Trend.FLAT
        change > 0 -> Trend.UP
        else -> Trend.DOWN
    }

    /** Template taking (amount, percent); the FLAT template ignores both. */
    @StringRes
    fun changeTemplate(trend: Trend): Int = when (trend) {
        Trend.UP -> R.string.change_up
        Trend.DOWN -> R.string.change_down
        Trend.FLAT -> R.string.change_none
    }
}
