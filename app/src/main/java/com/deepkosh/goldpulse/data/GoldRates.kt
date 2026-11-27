package com.deepkosh.goldpulse.data

import com.deepkosh.goldpulse.config.GoldConfig

/** International spot gold (USD per troy ounce) and the USD to INR exchange rate. */
data class MarketQuote(
    val spotUsdPerOunce: Double,
    val usdInr: Double,
) {
    /** Returns this quote, or null if either value is outside any realistic range (bad feed data). */
    fun takeIfPlausible(): MarketQuote? = takeIf {
        spotUsdPerOunce.isFinite() && spotUsdPerOunce in 200.0..100_000.0 &&
            usdInr.isFinite() && usdInr in 20.0..500.0
    }
}

/** The latest [MarketQuote], with the service that supplied it and when it was fetched. */
data class LiveQuote(
    val market: MarketQuote,
    val source: String,
    val fetchedAtMillis: Long,
)

enum class Karat(val purity: Double) {
    K24(GoldConfig.PURITY_24K),
    K22(GoldConfig.PURITY_22K),
    K18(GoldConfig.PURITY_18K),
}

/** Indicative INR rate for one karat, with yesterday's rate when known. */
data class KaratRate(
    val karat: Karat,
    val perGram: Double,
    val previousPerGram: Double?,
) {
    val perTenGrams: Double get() = perGram * 10

    val changePerGram: Double? get() = previousPerGram?.let { perGram - it }

    val changePerTenGrams: Double? get() = changePerGram?.times(10)

    val changePercent: Double?
        get() = previousPerGram?.takeIf { it > 0 }?.let { (perGram - it) / it * 100 }
}

object GoldCalculator {

    fun pricePerGram24k(quote: MarketQuote): Double =
        quote.spotUsdPerOunce / GoldConfig.GRAMS_PER_TROY_OUNCE *
            quote.usdInr *
            (1 + GoldConfig.IMPORT_DUTY_PERCENT / 100)

    fun karatRates(today: MarketQuote, yesterday: MarketQuote?): List<KaratRate> {
        val today24k = pricePerGram24k(today)
        val yesterday24k = yesterday?.let(::pricePerGram24k)
        return Karat.entries.map { karat ->
            KaratRate(
                karat = karat,
                perGram = today24k * karat.purity,
                previousPerGram = yesterday24k?.times(karat.purity),
            )
        }
    }
}

/** Everything the UI and the daily notification show. */
data class GoldRates(
    val live: LiveQuote,
    val yesterday: MarketQuote?,
) {
    val karats: List<KaratRate> = GoldCalculator.karatRates(live.market, yesterday)

    fun rate(karat: Karat): KaratRate = karats.first { it.karat == karat }
}
