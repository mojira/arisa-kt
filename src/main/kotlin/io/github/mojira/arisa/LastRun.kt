package io.github.mojira.arisa

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.commands.ShadowbanCommand
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class EpochMilliInstantConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == Instant::class.java
    override fun toJson(value: Any) = (value as Instant).toEpochMilli().toString()
    override fun fromJson(jv: JsonValue): Instant = jv.longValue?.let { long ->
        Instant.ofEpochMilli(long)
    } ?: LastRunFile.defaultTime()
}

val instantConverter = EpochMilliInstantConverter()

data class Shadowban(
    val user: String,
    val since: Instant,
    val until: Instant
) {
    fun banTimeContains(instant: Instant): Boolean = instant in since..until
}

data class LastRunFile(
    val time: Instant,
    val failedTickets: Set<String>,
    val shadowbans: List<Shadowban>
) {
    companion object {
        fun defaultTime(): Instant =
            Instant.now().minus(LastRun.DEFAULT_START_TIME_MINUTES_BEFORE_NOW, ChronoUnit.MINUTES)

        fun read(readFromFile: () -> String): LastRunFile {
            val result = Klaxon().converter(instantConverter).parse<LastRunFile>(readFromFile())

            return result ?: LastRunFile(
                time = defaultTime(),
                failedTickets = setOf(),
                shadowbans = listOf()
            )
        }
    }

    fun write(writeToFile: (String) -> Unit) {
        val result = Klaxon().converter(instantConverter).toJsonString(this)
        writeToFile(result)
    }
}

/**
 * Stores information about the previous run (start time, and tickets that failed during the run)
 */
class LastRun(
    private val readFromFile: () -> String,
    private val writeToFile: (String) -> Unit
) {
    companion object {
        const val DEFAULT_START_TIME_MINUTES_BEFORE_NOW = 5L
        const val SHADOWBAN_DURATION_IN_HOURS = 24L

        fun getLastRun(config: Config): LastRun {
            val lastRunFile = File("lastrun.json")
            if (!lastRunFile.exists()) migrateLegacyFile()

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

        // Migrate old last-run file
        private fun migrateLegacyFile() {
            val legacyFile = File("last-run")
            if (legacyFile.exists()) {
                val fileComponents = legacyFile.readText().trim().split(',')

                val time = if (fileComponents[0].isNotEmpty()) {
                    Instant.ofEpochMilli(fileComponents[0].toLong())
                } else {
                    LastRunFile.defaultTime()
                }

                val failedTickets = fileComponents.subList(1, fileComponents.size).toSet()

                val fileContents = LastRunFile(time, failedTickets, shadowbans = emptyList())
                fileContents.write {
                    val newFile = File("lastrun.json")
                    newFile.writeText(it)
                }

                legacyFile.delete()
            }
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

        LastRunFile(time, failedTickets, shadowbans).write(writeToFile)
    }

    private fun addShadowbannedUser(userName: String) {
        shadowbans.add(
            Shadowban(
                user = userName,
                since = time,
                until = time.plus(SHADOWBAN_DURATION_IN_HOURS, ChronoUnit.HOURS)
            )
        )
    }

    fun getCurrentlyShadowbannedUsers(): Map<String, Shadowban> =
        shadowbans
            .filter { it.banTimeContains(time) }
            .associateBy { it.user }

    init {
        val file = LastRunFile.read(readFromFile)
        time = file.time
        failedTickets = file.failedTickets
        shadowbans = file.shadowbans.toMutableList()

        // Initialize shadowban command
        ShadowbanCommand.addShadowbannedUser = this::addShadowbannedUser
    }
}
