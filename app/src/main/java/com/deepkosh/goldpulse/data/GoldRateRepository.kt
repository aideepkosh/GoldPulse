package com.deepkosh.goldpulse.data

import com.deepkosh.goldpulse.config.GoldConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Single entry point for rates, shared by the UI and the daily worker. */
class GoldRateRepository(
    private val sources: RateSources,
    private val cache: RateCache,
) {
    private val zone: ZoneId = ZoneId.of(GoldConfig.RATE_TIME_ZONE)
    private val mutex = Mutex()

    /** Last saved rates, or null if the app has never fetched successfully. */
    suspend fun savedRates(): GoldRates? = mutex.withLock {
        withContext(Dispatchers.IO) { cache.latest()?.let(::withYesterday) }
    }

    /** Fetches and saves fresh rates. Throws [IOException] if no source is reachable. */
    suspend fun refresh(): GoldRates = mutex.withLock {
        withContext(Dispatchers.IO) {
            val live = sources.fetchLatest()
            val today = live.day()
            cache.saveLatest(live, today)

            // First run, or the app wasn't opened yesterday: use the published daily snapshot.
            val yesterday = today.minusDays(1)
            if (cache.quoteFor(yesterday) == null) {
                sources.fetchDaily(yesterday)?.let { cache.saveQuoteFor(yesterday, it) }
            }
            withYesterday(live)
        }
    }

    private fun withYesterday(live: LiveQuote) =
        GoldRates(live = live, yesterday = cache.quoteFor(live.day().minusDays(1)))

    private fun LiveQuote.day(): LocalDate =
        Instant.ofEpochMilli(fetchedAtMillis).atZone(zone).toLocalDate()
}
