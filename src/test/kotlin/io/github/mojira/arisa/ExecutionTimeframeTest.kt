package io.github.mojira.arisa

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ExecutionTimeframeTest : StringSpec({
    "getTimeframeFromLastRun should return the correct timeframe if last run was recently" {
        val lastRunTime = Instant.now()
            .minus(1, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MILLIS)

        val lastRun = LastRun(
            { LastRunFile(lastRunTime, emptySet(), emptyList()) },
            { }
        )

        val timeframe = ExecutionTimeframe.getTimeframeFromLastRun(lastRun)

        timeframe.lastRunTime shouldBe lastRunTime
        timeframe.currentRunTime.isAfter(Instant.now()) shouldBe false
        // Should not be capped
        timeframe.getFreshlyUpdatedJql() shouldNotContain " AND created <= "

        val delayedStart = LocalDateTime.of(2021, 1, 1, 12, 0, 0)
            .atZone(ZoneOffset.UTC)
            .toInstant()
        val delayedEnd = delayedStart.plus(Duration.between(timeframe.lastRunTime, timeframe.currentRunTime))
        val offset = Duration.between(delayedStart, timeframe.lastRunTime)
        // Shift timeframe to start at `offsetBaseInstant`
        // Note: Cannot hardcode `delayedEnd` value in string because it depends on how fast
        // `ExecutionTimeframe.getTimeframeFromLastRun` executes
        timeframe.getDelayedUpdatedJql(offset) shouldBe "created > 1609502400000 AND created <= ${delayedEnd.toEpochMilli()}"
    }

    "getTimeframeFromLastRun should return the correct timeframe if last run was a while ago" {
        val lastRunTime = LocalDateTime.of(2021, 1, 1, 12, 0, 0)
            .atZone(ZoneOffset.UTC)
            .toInstant()

        val timeframeEnd = lastRunTime
            .plus(ExecutionTimeframe.MAX_TIMEFRAME_DURATION_IN_MINUTES, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MILLIS)

        val lastRun = LastRun(
            { LastRunFile(lastRunTime, emptySet(), emptyList()) },
            { }
        )

        val timeframe = ExecutionTimeframe.getTimeframeFromLastRun(lastRun)

        timeframe.lastRunTime shouldBe lastRunTime
        timeframe.currentRunTime shouldBe timeframeEnd
        timeframe.getFreshlyUpdatedJql() shouldBe "created > 1609502400000 AND created <= 1609503000000"
        timeframe.getDelayedUpdatedJql(Duration.ofHours(1)) shouldBe "created > 1609498800000 AND created <= 1609499400000"
    }
})
