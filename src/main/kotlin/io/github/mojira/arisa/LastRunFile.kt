package io.github.mojira.arisa

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class EpochMilliInstantConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == Instant::class.java
    override fun toJson(value: Any) = '"' + (value as Instant).toEpochMilli().toString() + '"'
    override fun fromJson(jv: JsonValue): Instant = jv.string?.let { str ->
        Instant.ofEpochMilli(str.toLong())
    } ?: run {
        println("Can't read $jv from json")
        LastRunFile.defaultTime()
    }
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
    val time: Instant? = defaultTime(),
    val failedTickets: Set<String>? = emptySet(),
    val shadowbans: List<Shadowban>? = emptyList()
) {
    companion object {
        fun defaultTime(): Instant =
            Instant.now().minus(LastRun.DEFAULT_START_TIME_MINUTES_BEFORE_NOW, ChronoUnit.MINUTES)

        fun read(readFromFile: () -> String): LastRunFile {
            val default = LastRunFile(
                time = defaultTime(),
                failedTickets = setOf(),
                shadowbans = listOf()
            )

            @SuppressWarnings("SwallowedException")
            val result = try {
                Klaxon().converter(instantConverter).parse<LastRunFile>(readFromFile())
            } catch (e: KlaxonException) {
                default
            }

            return result ?: default
        }
    }

    fun write(writeToFile: (String) -> Unit) {
        val result = Klaxon().converter(instantConverter).toJsonString(this)
        writeToFile(result)
    }
}

class LastRunFileService(
    private val fileName: String,
    private val legacyFileName: String
) {
    fun writeLastRunFile(file: LastRunFile) {
        val lastRunFile = File(fileName)
        file.write(lastRunFile::writeText)
    }

    fun getLastRunFile(): LastRunFile {
        val lastRunFile = File(fileName)
        if (!lastRunFile.exists()) migrateLegacyFile()

        return LastRunFile.read(lastRunFile::readText)
    }

    // Migrate old last-run file
    private fun migrateLegacyFile() {
        val legacyFile = File(legacyFileName)

        val legacyContents = if (legacyFile.exists()) legacyFile.readText() else ""

        val newFileContents = convertLegacyFile(legacyContents)
        newFileContents.write {
            val newFile = File(fileName)
            newFile.writeText(it)
        }

        legacyFile.delete()
    }

    companion object {
        fun convertLegacyFile(legacyContents: String): LastRunFile {
            val fileComponents = legacyContents.trim().split(',')

            val time = if (fileComponents[0].isNotEmpty()) {
                Instant.ofEpochMilli(fileComponents[0].toLong())
            } else {
                LastRunFile.defaultTime()
            }

            val failedTickets = fileComponents.subList(1, fileComponents.size).toSet()

            return LastRunFile(time, failedTickets, shadowbans = emptyList())
        }
    }
}
