package io.github.mojira.arisa

import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stores information about the previous run (start time, and tickets that failed during the run)
 */
object LastRun {
    var time: Instant
    var failedTickets: Set<String>

    private val lastRunFile = File("last-run")

    private const val DEFAULT_START_TIME_MINUTES_BEFORE_NOW = 5L

    fun writeToFile() {
        val failedString = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

        lastRunFile.writeText("${time.toEpochMilli()}$failedString")
    }

    fun update(newTime: Instant, newFailedTickets: Set<String>) {
        time = newTime
        failedTickets = newFailedTickets
    }

    init {
        val fileContents = if (lastRunFile.exists()) { lastRunFile.readText() } else ""

        val lastRunFileComponents = fileContents.trim().split(',')

        time = if (lastRunFileComponents[0].isNotEmpty()) {
            Instant.ofEpochMilli(lastRunFileComponents[0].toLong())
        } else {
            Instant.now().minus(DEFAULT_START_TIME_MINUTES_BEFORE_NOW, ChronoUnit.MINUTES)
        }

        failedTickets = lastRunFileComponents.subList(1, lastRunFileComponents.size).toSet()
    }
}
