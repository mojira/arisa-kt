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
    private val readFromFile: () -> String,
    private val writeToFile: (String) -> Unit
) {
    companion object {
        const val DEFAULT_START_TIME_MINUTES_BEFORE_NOW = 5L

        fun getLastRun(config: Config): LastRun {
            val lastRunFile = File("last-run")
            return LastRun(
                readFromFile = {
                    if (lastRunFile.exists()) lastRunFile.readText()
                    else ""
                },
                writeToFile = { contents ->
                    if (config[Arisa.Debug.updateLastRun]) {
                        lastRunFile.writeText(contents)
                    }
                }
            )
        }
    }

    var time: Instant
    var failedTickets: Set<String>

    /**
     * Updates last run and writes it to the `last-run` file
     */
    fun update(newTime: Instant, newFailedTickets: Set<String>) {
        time = newTime
        failedTickets = newFailedTickets

        val failedString = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

        writeToFile("${time.toEpochMilli()}$failedString")
    }

    init {
        val lastRunFileComponents = readFromFile().trim().split(',')

        time = if (lastRunFileComponents[0].isNotEmpty()) {
            Instant.ofEpochMilli(lastRunFileComponents[0].toLong())
        } else {
            Instant.now().minus(DEFAULT_START_TIME_MINUTES_BEFORE_NOW, ChronoUnit.MINUTES)
        }

        failedTickets = lastRunFileComponents.subList(1, lastRunFileComponents.size).toSet()
    }
}
