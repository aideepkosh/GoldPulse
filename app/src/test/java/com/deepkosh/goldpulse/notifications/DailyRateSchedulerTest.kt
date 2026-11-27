package com.deepkosh.goldpulse.notifications

import com.deepkosh.goldpulse.config.GoldConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyRateSchedulerTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 11, 27, hour, minute, 0, 0, ist)
    private val runHour = GoldConfig.NOTIFICATION_HOUR

    @Test
    fun `before the run time, waits until today's run`() {
        val delay = DailyRateScheduler.delayUntilNextRun(at(runHour - 2))
        assertEquals(Duration.ofHours(2).toMillis(), delay)
    }

    @Test
    fun `at or after the run time, waits until tomorrow`() {
        assertEquals(Duration.ofHours(24).toMillis(), DailyRateScheduler.delayUntilNextRun(at(runHour)))
        assertEquals(Duration.ofHours(23).toMillis(), DailyRateScheduler.delayUntilNextRun(at(runHour + 1)))
    }

    @Test
    fun `a run that fires slightly early still schedules tomorrow`() {
        val now = at(runHour - 1, 59)
        val delay = DailyRateScheduler.delayUntilNextRun(now, notBefore = now.plusHours(1))
        assertEquals(Duration.ofHours(24).plusMinutes(1).toMillis(), delay)
    }
}
