package com.deepkosh.goldpulse.data

import com.deepkosh.goldpulse.config.GoldConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoldCalculatorTest {

    private val today = MarketQuote(spotUsdPerOunce = 3110.35, usdInr = 90.0)

    @Test
    fun `24K per gram is spot per gram times USD-INR plus import duty`() {
        // 3110.35 / 31.1035 = 100 USD per gram -> 9000 INR -> plus duty
        val expected = 9000.0 * (1 + GoldConfig.IMPORT_DUTY_PERCENT / 100)
        assertEquals(expected, GoldCalculator.pricePerGram24k(today), 1e-6)
    }

    @Test
    fun `22K and 18K apply their purity to 24K`() {
        val rates = GoldRates(LiveQuote(today, "test", 0L), yesterday = null)
        val k24 = rates.rate(Karat.K24).perGram
        assertEquals(k24 * 0.916, rates.rate(Karat.K22).perGram, 1e-6)
        assertEquals(k24 * 0.75, rates.rate(Karat.K18).perGram, 1e-6)
        assertEquals(k24 * 10, rates.rate(Karat.K24).perTenGrams, 1e-6)
    }

    @Test
    fun `change vs yesterday is reported per gram, per 10 g and in percent`() {
        val yesterday = today.copy(spotUsdPerOunce = 3110.35 / 1.02)
        val rate = GoldRates(LiveQuote(today, "test", 0L), yesterday).rate(Karat.K22)
        val previous = rate.previousPerGram!!
        assertEquals(rate.perGram - previous, rate.changePerGram!!, 1e-6)
        assertEquals((rate.perGram - previous) * 10, rate.changePerTenGrams!!, 1e-6)
        assertEquals(2.0, rate.changePercent!!, 1e-9)
    }

    @Test
    fun `no yesterday means no change`() {
        val rate = GoldRates(LiveQuote(today, "test", 0L), yesterday = null).rate(Karat.K24)
        assertNull(rate.changePerGram)
        assertNull(rate.changePercent)
    }

    @Test
    fun `implausible feed values are rejected`() {
        assertNull(MarketQuote(spotUsdPerOunce = 0.0, usdInr = 90.0).takeIfPlausible())
        assertNull(MarketQuote(spotUsdPerOunce = 3000.0, usdInr = 1.0).takeIfPlausible())
        assertNull(MarketQuote(spotUsdPerOunce = Double.POSITIVE_INFINITY, usdInr = 90.0).takeIfPlausible())
        assertEquals(today, today.takeIfPlausible())
    }
}
