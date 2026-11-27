package com.deepkosh.goldpulse.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * Saves the last fetched quote (for offline use) and one quote per day (for "change vs yesterday").
 * Stores raw spot and USD/INR values, so editing GoldConfig changes cached rates too.
 */
class RateCache(context: Context) {

    private val prefs = context.getSharedPreferences("rate_cache", Context.MODE_PRIVATE)

    fun latest(): LiveQuote? {
        val json = prefs.getString(KEY_LATEST, null) ?: return null
        return try {
            val obj = JSONObject(json)
            LiveQuote(
                market = obj.getJSONObject("market").toMarketQuote(),
                source = obj.getString("source"),
                fetchedAtMillis = obj.getLong("fetchedAt"),
            )
        } catch (_: JSONException) {
            null
        }
    }

    /** Saves [quote] as the latest rate and as the rate for [day] (later fetches on the same day win). */
    fun saveLatest(quote: LiveQuote, day: LocalDate) {
        val latest = JSONObject()
            .put("market", quote.market.toJson())
            .put("source", quote.source)
            .put("fetchedAt", quote.fetchedAtMillis)
        val history = history().put(day.toString(), quote.market.toJson())
        prefs.edit {
            putString(KEY_LATEST, latest.toString())
            putString(KEY_HISTORY, history.trimmed().toString())
        }
    }

    fun quoteFor(day: LocalDate): MarketQuote? = try {
        history().optJSONObject(day.toString())?.toMarketQuote()
    } catch (_: JSONException) {
        null
    }

    /** Stores a quote for a past [day] that the app never fetched itself. */
    fun saveQuoteFor(day: LocalDate, quote: MarketQuote) {
        val history = history().put(day.toString(), quote.toJson())
        prefs.edit { putString(KEY_HISTORY, history.trimmed().toString()) }
    }

    private fun history(): JSONObject = try {
        JSONObject(prefs.getString(KEY_HISTORY, null) ?: "{}")
    } catch (_: JSONException) {
        JSONObject()
    }

    /** Keeps only the most recent [HISTORY_DAYS] entries. ISO dates sort chronologically as strings. */
    private fun JSONObject.trimmed(): JSONObject {
        val keep = keys().asSequence().sortedDescending().take(HISTORY_DAYS).toSet()
        return JSONObject().also { out ->
            keep.forEach { day -> optJSONObject(day)?.let { out.put(day, it) } }
        }
    }

    private fun MarketQuote.toJson() = JSONObject()
        .put("spot", spotUsdPerOunce)
        .put("usdInr", usdInr)

    private fun JSONObject.toMarketQuote() = MarketQuote(
        spotUsdPerOunce = getDouble("spot"),
        usdInr = getDouble("usdInr"),
    )

    private companion object {
        const val KEY_LATEST = "latest"
        const val KEY_HISTORY = "history"
        const val HISTORY_DAYS = 14
    }
}
