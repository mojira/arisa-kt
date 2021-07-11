package io.github.mojira.arisa

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExecutionTimeframeTest : StringSpec({
    "getTimeframeFromLastRun should return the correct timeframe if last run was recently" {
        val lastRunTime = Instant.now()
            .minus(1, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MILLIS)

        val lastRun = LastRun(
            { lastRunTime.toEpochMilli().toString() },
            { }
        )

        val timeframe = ExecutionTimeframe.getTimeframeFromLastRun(lastRun)

        val timeframeEnd = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        timeframe.lastRunTime shouldBe lastRunTime
        timeframe.currentRunTime.isAfter(timeframeEnd) shouldBe false
        timeframe.capIfNotOpenEnded() shouldBe ""
        timeframe.duration() shouldBe Duration.between(lastRunTime, timeframeEnd).abs()
    }

    "getTimeframeFromLastRun should return the correct timeframe if last run was a while ago" {
        val offsetInMinutes = 20L

        val lastRunTime = Instant.now()
            .minus(offsetInMinutes, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MILLIS)

        val timeframeEnd = lastRunTime
            .plus(ExecutionTimeframe.MAX_TIMEFRAME_DURATION_IN_MINUTES, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MILLIS)

        val lastRun = LastRun(
            { lastRunTime.toEpochMilli().toString() },
            { }
        )

        val timeframe = ExecutionTimeframe.getTimeframeFromLastRun(lastRun)

        timeframe.lastRunTime shouldBe lastRunTime
        timeframe.currentRunTime.isAfter(timeframeEnd) shouldBe false
        timeframe.capIfNotOpenEnded() shouldBe " AND updated <= ${ timeframeEnd.toEpochMilli() }"
        timeframe.duration() shouldBe Duration.between(lastRunTime, timeframeEnd).abs()
    }
})
