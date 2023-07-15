package io.github.mojira.arisa

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.time.Instant
import java.time.temporal.ChronoUnit

class LastRunTest : StringSpec({
    "should use default time before now if input file is empty" {
        val lastRunFile = LastRunFile.read { "" }

        val time = LastRunFile.defaultTime()

        lastRunFile.time?.isAfter(time) shouldBe false
        lastRunFile.failedTickets.shouldBeEmpty()
    }

    "should use default time before now if input file is invalid JSON" {
        val lastRunFile = LastRunFile.read { "{ hey this is invalid json }" }

        val time = LastRunFile.defaultTime()

        lastRunFile.time?.isAfter(time) shouldBe false
        lastRunFile.failedTickets.shouldBeEmpty()
    }

    "should read time from input file" {
        val time = Instant.ofEpochMilli(123456789)

        val lastRunFile = LastRunFile.read { """{"time": "123456789"}""" }

        lastRunFile.time shouldBe time
        lastRunFile.failedTickets.shouldBeEmpty()
    }

    "should read failed tickets from input file" {
        val time = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val tickets = listOf("MC-1234", "MCL-5678", "MCPE-9012")

        val lastRun = LastRunFile.read {
            """{"time": "${time.toEpochMilli()}", "failedTickets": ["MC-1234", "MCL-5678", "MCPE-9012"]}"""
        }

        lastRun.time shouldBe time
        lastRun.failedTickets shouldContainExactly tickets
    }

    "should read shadowbans from input file" {
        val time = Instant.ofEpochMilli(222222222222)

        val shadowbans = listOf(
            Shadowban(
                user = "Spammer Mc Spamface",
                since = Instant.ofEpochMilli(1000000),
                until = Instant.ofEpochMilli(2000000)
            ),
            Shadowban(
                user = "Monty Python",
                since = Instant.ofEpochMilli(3000000),
                until = Instant.ofEpochMilli(4000000)
            ),
            Shadowban(
                user = "Nigerian Prince",
                since = Instant.ofEpochMilli(5000000),
                until = Instant.ofEpochMilli(6000000)
            )
        )

        val lastRun = LastRunFile.read {
            """{
                "time": "222222222222",
                "shadowbans": [
                    {"user": "Spammer Mc Spamface", "since": "1000000", "until": "2000000"},
                    {"user": "Monty Python", "since": "3000000", "until": "4000000"},
                    {"user": "Nigerian Prince", "since": "5000000", "until": "6000000"}
                ]
            }
            """.trimMargin()
        }

        lastRun.time shouldBe time
        lastRun.shadowbans shouldContainExactly shadowbans
    }

    "should write to file properly" {
        val shadowbans = listOf(
            Shadowban(
                user = "Spammer Mc Spamface",
                since = Instant.ofEpochMilli(1000000),
                until = Instant.ofEpochMilli(2000000)
            ),
            Shadowban(
                user = "Monty Python",
                since = Instant.ofEpochMilli(3000000),
                until = Instant.ofEpochMilli(4000000)
            ),
            Shadowban(
                user = "Nigerian Prince",
                since = Instant.ofEpochMilli(5000000),
                until = Instant.ofEpochMilli(6000000)
            )
        )

        val lastRunFile = LastRunFile(
            time = Instant.ofEpochMilli(123456789),
            failedTickets = setOf("MC-1234", "WEB-12345", "MCPE-123"),
            shadowbans = shadowbans
        )

        var written = ""
        lastRunFile.write { written = it }

        written.shouldNotBeEmpty()

        val newLastRunFile = LastRunFile.read { written }
        newLastRunFile shouldBe lastRunFile
    }

    "should update time and failed tickets" {
        val time = Instant.now()
        val tickets = setOf("MC-1234", "MCL-5678", "MCPE-9012")

        var writtenToFile = false
        var file = LastRunFile(time, tickets, emptyList())

        val lastRun = LastRun(
            { file },
            { newFile ->
                writtenToFile = true
                file = newFile
            }
        )

        val newTime = Instant.now()
        val newTickets = setOf("MC-4", "MCPE-9012")
        val newFile = LastRunFile(newTime, newTickets, emptyList())

        lastRun.update(newTime, newTickets)

        lastRun.time shouldBe newTime
        lastRun.failedTickets shouldContainExactly newTickets

        writtenToFile shouldBe true
        file shouldBe newFile
    }

    "should update shadowbans" {
        val time = Instant.ofEpochMilli(0)

        var shadowbans: List<Shadowban>? = null

        val lastRun = LastRun(
            {
                LastRunFile(
                    time,
                    emptySet(),
                    listOf(
                        Shadowban(
                            user = "Spammer Mc Spamface",
                            since = Instant.ofEpochMilli(1000000),
                            until = Instant.ofEpochMilli(2000000)
                        ),
                        Shadowban(
                            user = "Monty Python",
                            since = Instant.ofEpochMilli(4000000),
                            until = Instant.ofEpochMilli(6000000)
                        )
                    )
                )
            },
            { newFile -> shadowbans = newFile.shadowbans }
        )

        val newTime = Instant.ofEpochMilli(5000000)
        lastRun.update(newTime, emptySet())

        lastRun.getShadowbannedUsers() shouldBe mapOf(
            "Monty Python" to Shadowban(
                user = "Monty Python",
                since = Instant.ofEpochMilli(4000000),
                until = Instant.ofEpochMilli(6000000)
            )
        )
        shadowbans shouldBe listOf(
            Shadowban(
                user = "Monty Python",
                since = Instant.ofEpochMilli(4000000),
                until = Instant.ofEpochMilli(6000000)
            )
        )
    }

    "should add shadowbans" {
        val time = Instant.ofEpochMilli(0)

        var shadowbans: List<Shadowban>? = null

        val lastRun = LastRun(
            {
                LastRunFile(
                    time,
                    emptySet(),
                    listOf(
                        Shadowban(
                            user = "Spammer Mc Spamface",
                            since = Instant.ofEpochMilli(1000000),
                            until = Instant.ofEpochMilli(2000000)
                        )
                    )
                )
            },
            { newFile -> shadowbans = newFile.shadowbans }
        )

        lastRun.addShadowbannedUser("Nigerian Prince")
        val aDayLater = time.plus(24, ChronoUnit.HOURS)

        lastRun.update(Instant.ofEpochMilli(0), emptySet())

        lastRun.getShadowbannedUsers() shouldBe mapOf(
            "Spammer Mc Spamface" to Shadowban(
                user = "Spammer Mc Spamface",
                since = Instant.ofEpochMilli(1000000),
                until = Instant.ofEpochMilli(2000000)
            ),
            "Nigerian Prince" to Shadowban(
                user = "Nigerian Prince",
                since = time,
                until = aDayLater
            )
        )

        shadowbans shouldContainExactly listOf(
            Shadowban(
                user = "Spammer Mc Spamface",
                since = Instant.ofEpochMilli(1000000),
                until = Instant.ofEpochMilli(2000000)
            ),
            Shadowban(
                user = "Nigerian Prince",
                since = time,
                until = aDayLater
            )
        )
    }

    "legacy last-run file upgrade should work properly" {
        LastRunFileService.convertLegacyFile("123456789,MC-1234,WEB-234") shouldBe LastRunFile(
            Instant.ofEpochMilli(123456789),
            setOf("MC-1234", "WEB-234"),
            emptyList()
        )

        LastRunFileService.convertLegacyFile("123456789") shouldBe LastRunFile(
            Instant.ofEpochMilli(123456789),
            emptySet(),
            emptyList()
        )

        val now = Instant.now()
        LastRunFileService.convertLegacyFile("").time?.isBefore(now) shouldBe true
    }
})
