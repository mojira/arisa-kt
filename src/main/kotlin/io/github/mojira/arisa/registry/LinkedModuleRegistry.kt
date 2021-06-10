package io.github.mojira.arisa.registry

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.UpdateLinkedModule
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * This class is the registry for the UpdateLinkedModule.
 * It only updates the linked field once if it's not set, and otherwise only updates it at most once per day.
 * This is done in order to avoid spam.
 */
class LinkedModuleRegistry(config: Config) : ModuleRegistry(config) {
    override val getJql = { lastRun: Instant ->
        val now = Instant.now()
        val intervalStart = now.minus(config[Arisa.Modules.UpdateLinked.updateIntervalHours], ChronoUnit.HOURS)
        val intervalEnd = intervalStart.minusMillis(now.toEpochMilli() - lastRun.toEpochMilli())
        "updated > ${lastRun.toEpochMilli()} OR (updated < ${intervalStart.toEpochMilli()}" +
            " AND updated > ${intervalEnd.toEpochMilli()})"
    }

    init {
        register(
            Arisa.Modules.UpdateLinked,
            UpdateLinkedModule(config[Arisa.Modules.UpdateLinked.updateIntervalHours])
        )
    }
}
