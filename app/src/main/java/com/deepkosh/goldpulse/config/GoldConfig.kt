package com.deepkosh.goldpulse.config

/**
 * All the numbers that drive GoldPulse's rate calculation, in one place.
 *
 * 24K INR per gram = spot (USD per troy ounce) / GRAMS_PER_TROY_OUNCE x USD/INR x (1 + IMPORT_DUTY_PERCENT / 100)
 * Other karats      = 24K x purity
 */
object GoldConfig {

    /** Grams in one troy ounce. Spot gold (XAU) is quoted in USD per troy ounce. */
    const val GRAMS_PER_TROY_OUNCE = 31.1035

    /**
     * Effective India import duty on gold, in percent.
     * 6% = 5% Basic Customs Duty + 1% AIDC (rates set in the July 2024 Union Budget).
     * Check the latest CBIC notification after each Budget and update this value.
     */
    const val IMPORT_DUTY_PERCENT = 6.0

    /** Purity of each karat, as a fraction of 24K. */
    const val PURITY_24K = 1.0
    const val PURITY_22K = 0.916
    const val PURITY_18K = 0.75

    /** Daily notification time, in the device's local time. */
    const val NOTIFICATION_HOUR = 9
    const val NOTIFICATION_MINUTE = 0

    /** Time zone that defines "today" and "yesterday" for rate comparisons. */
    const val RATE_TIME_ZONE = "Asia/Kolkata"

    /** Auto-refresh when the app comes to the foreground and the saved rate is older than this. */
    const val STALE_AFTER_MINUTES = 30L
}
