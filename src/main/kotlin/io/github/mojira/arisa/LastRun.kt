package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stores information about the previous run (start time, and tickets that failed during the run)
 */
class LastRun(
    private val config: Config
) {
    companion object {
        private const val DEFAULT_START_TIME_MINUTES_BEFORE_NOW = 5L
    }

    var time: Instant
    var failedTickets: Set<String>

    private val lastRunFile = File("last-run")

    private fun writeToFile() {
        val failedString = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

        lastRunFile.writeText("${time.toEpochMilli()}$failedString")
    }

    /**
     * Updates last run and writes it to the `last-run` file
     */
    fun update(newTime: Instant, newFailedTickets: Set<String>) {
        time = newTime
        failedTickets = newFailedTickets

        if (config[Arisa.Debug.updateLastRun]) {
            writeToFile()
        }
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
