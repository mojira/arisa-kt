package io.github.mojira.arisa.infrastructure.services

import io.github.mojira.arisa.TIME_MINUTES
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

object LastRunService {
    val lastRunFile = File("last-run")

    fun readLastRunTime(lastRun: List<String>) = when {
        lastRun[0].isNotEmpty() -> Instant.ofEpochMilli(lastRun[0].toLong())
        else -> Instant.now().minus(TIME_MINUTES, ChronoUnit.MINUTES)
    }

    fun readLastRun(): List<String> = when {
        lastRunFile.exists() -> lastRunFile.readText().trim().split(",")
        else -> emptyList()
    }

    fun writeLastRun(curRunTime: Instant, failed: String) {
        lastRunFile.writeText("${curRunTime.toEpochMilli()}$failed")
    }
}