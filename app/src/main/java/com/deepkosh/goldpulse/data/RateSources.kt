package com.deepkosh.goldpulse.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

/**
 * Free rate providers that need no API key. Each endpoint was checked with curl before use.
 *
 * - gold-api.com: live XAU price, plus the USD/INR rate it uses for its INR quote.
 * - Frankfurter (frankfurter.dev): ECB reference USD/INR, updated each working day.
 * - fawazahmed0/currency-api: daily XAU and INR per USD, with dated history,
 *   served from two mirrors (jsDelivr and Cloudflare Pages).
 */
class RateSources(private val http: HttpFetcher = HttpFetcher()) {

    /** Latest spot price and exchange rate. Throws [IOException] if no source answers. */
    suspend fun fetchLatest(): LiveQuote = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        goldApi()?.let { return@withContext LiveQuote(it, SOURCE_GOLD_API, now) }

        // Fallback: daily snapshot for spot, ECB for USD/INR if available.
        val daily = currencyApi(LATEST) ?: throw IOException("No gold price source is reachable")
        val ecbUsdInr = frankfurterUsdInr()
        if (ecbUsdInr != null) {
            LiveQuote(daily.copy(usdInr = ecbUsdInr), "$SOURCE_CURRENCY_API, $SOURCE_FRANKFURTER", now)
        } else {
            LiveQuote(daily, SOURCE_CURRENCY_API, now)
        }
    }

    /** Daily snapshot for [date]; used for "yesterday" when the app has no saved rate for that day. */
    suspend fun fetchDaily(date: LocalDate): MarketQuote? = withContext(Dispatchers.IO) {
        currencyApi(date.toString())
    }

    private fun goldApi(): MarketQuote? = attempt(SOURCE_GOLD_API) {
        // {"currency":"INR","exchangeRate":92.52,"price":336856.28,"symbol":"XAU",...}
        val json = JSONObject(http.get(GOLD_API_XAU_INR))
        if (json.getString("currency") != "INR" || json.getString("symbol") != "XAU") {
            throw JSONException("Unexpected gold-api.com payload")
        }
        val usdInr = json.getDouble("exchangeRate")
        MarketQuote(spotUsdPerOunce = json.getDouble("price") / usdInr, usdInr = usdInr)
            .takeIfPlausible()
    }

    private fun frankfurterUsdInr(): Double? = attempt(SOURCE_FRANKFURTER) {
        // {"amount":1.0,"base":"USD","date":"2026-11-26","rates":{"INR":92.45}}
        JSONObject(http.get(FRANKFURTER_USD_INR)).getJSONObject("rates").getDouble("INR")
            .takeIf { it.isFinite() && it in 20.0..500.0 }
    }

    /** [version] is "latest" or an ISO date such as "2026-11-26". */
    private fun currencyApi(version: String): MarketQuote? {
        for (mirror in CURRENCY_API_MIRRORS) {
            val quote = attempt(SOURCE_CURRENCY_API) {
                // {"date":"2026-11-26","usd":{"inr":92.50,"xau":0.00028,...}}
                val usd = JSONObject(http.get(mirror.format(version))).getJSONObject("usd")
                MarketQuote(spotUsdPerOunce = 1 / usd.getDouble("xau"), usdInr = usd.getDouble("inr"))
                    .takeIfPlausible()
            }
            if (quote != null) return quote
        }
        return null
    }

    private inline fun <T : Any> attempt(name: String, block: () -> T?): T? = try {
        block()
    } catch (e: IOException) {
        Log.w(TAG, "$name unavailable: ${e.message}")
        null
    } catch (e: JSONException) {
        Log.w(TAG, "$name returned unexpected data: ${e.message}")
        null
    }

    private companion object {
        const val TAG = "RateSources"
        const val LATEST = "latest"

        const val SOURCE_GOLD_API = "gold-api.com"
        const val SOURCE_FRANKFURTER = "Frankfurter"
        const val SOURCE_CURRENCY_API = "currency-api"

        const val GOLD_API_XAU_INR = "https://api.gold-api.com/price/XAU/INR"
        const val FRANKFURTER_USD_INR = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=INR"
        val CURRENCY_API_MIRRORS = listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@%s/v1/currencies/usd.min.json",
            "https://%s.currency-api.pages.dev/v1/currencies/usd.min.json",
        )
    }
}
