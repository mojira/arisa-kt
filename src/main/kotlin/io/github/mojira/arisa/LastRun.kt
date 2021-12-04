package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.commands.ShadowbanCommand
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stores information about the previous run (start time, and tickets that failed during the run)
 */
class LastRun(
    private val readFromFile: () -> LastRunFile,
    private val writeToFile: (LastRunFile) -> Unit
) {
    companion object {
        const val DEFAULT_START_TIME_MINUTES_BEFORE_NOW = 5L
        const val SHADOWBAN_DURATION_IN_HOURS = 24L

        private val lastRunFileService = LastRunFileService("lastrun.json", "last-run")

        fun getLastRun(config: Config): LastRun {
            return LastRun(
                readFromFile = { lastRunFileService.getLastRunFile() },
                writeToFile = { file ->
                    if (config[Arisa.Debug.updateLastRun]) {
                        lastRunFileService.writeLastRunFile(file)
                    }
                }
            )
        }
    }

    var time: Instant
    var failedTickets: Set<String>
    private var shadowbans: MutableList<Shadowban>

    /**
     * Updates last run and writes it to the `lastrun.json` file
     */
    fun update(newTime: Instant, newFailedTickets: Set<String>) {
        time = newTime
        failedTickets = newFailedTickets
        shadowbans.removeIf { it.until.isBefore(newTime) }

        writeToFile(LastRunFile(time, failedTickets, shadowbans))
    }

    fun addShadowbannedUser(userName: String) {
        shadowbans.add(
            Shadowban(
                user = userName,
                since = time,
                until = time.plus(SHADOWBAN_DURATION_IN_HOURS, ChronoUnit.HOURS)
            )
        )
    }

    fun getShadowbannedUsers(): Map<String, Shadowban> =
        // We want the earliest applicable ban frame for a particular user.
        // Since `associateBy` always picks the last one, we'll reverse the list.
        // Shadowbans that are no longer active get removed through `update`, so we don't need to worry about those.
        shadowbans
            .reversed()
            .associateBy { it.user }

    init {
        val file = readFromFile()
        time = file.time ?: LastRunFile.defaultTime()
        failedTickets = file.failedTickets ?: emptySet()
        shadowbans = file.shadowbans?.toMutableList() ?: mutableListOf()

        // Initialize shadowban command
        ShadowbanCommand.addShadowbannedUser = this::addShadowbannedUser
    }
}
