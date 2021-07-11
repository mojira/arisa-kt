package io.github.mojira.arisa

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class LastRunTest : StringSpec({
    "should use default time before now if input file is empty" {
        val lastRun = LastRun(
            { "" },
            { }
        )

        val time = Instant.now().minus(LastRun.DEFAULT_START_TIME_MINUTES_BEFORE_NOW, ChronoUnit.MINUTES)

        lastRun.time.isAfter(time) shouldBe false
        lastRun.failedTickets.shouldBeEmpty()
    }

    "should read time from input file" {
        val time = Instant.ofEpochMilli(123456789)

        val lastRun = LastRun(
            { time.toEpochMilli().toString() },
            { }
        )

        lastRun.time shouldBe time
        lastRun.failedTickets.shouldBeEmpty()
    }

    "should read failed tickets from input file" {
        val time = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val tickets = listOf("MC-1234", "MCL-5678", "MCPE-9012")

        val lastRun = LastRun(
            { "${ time.toEpochMilli() },${ tickets.joinToString(",") }" },
            { }
        )

        lastRun.time shouldBe time
        lastRun.failedTickets shouldContainExactly tickets
    }

    "should update time and failed tickets" {
        val time = Instant.now()
        val tickets = listOf("MC-1234", "MCL-5678", "MCPE-9012")

        var writtenToFile = false
        var fileContents = "${ time.toEpochMilli() },${ tickets.joinToString(",") }"

        val lastRun = LastRun(
            { fileContents },
            { contents ->
                writtenToFile = true
                fileContents = contents
            }
        )

        val newTime = Instant.now()
        val newTickets = setOf("MC-4", "MCPE-9012")
        val newFileContents = "${ newTime.toEpochMilli() },${ newTickets.joinToString(",") }"

        lastRun.update(newTime, newTickets)

        lastRun.time shouldBe newTime
        lastRun.failedTickets shouldContainExactly newTickets

        writtenToFile shouldBe true
        fileContents shouldBe newFileContents
    }
})
