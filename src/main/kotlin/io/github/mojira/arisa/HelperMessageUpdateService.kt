package io.github.mojira.arisa

import io.github.mojira.arisa.infrastructure.HelperMessageService
import java.io.File
import java.time.Duration
import java.time.Instant

class HelperMessageUpdateService(private val helperMessageService: HelperMessageService) {
    companion object {
        private const val UPDATE_INTERVAL_IN_SECONDS = 60 * 60L // 1 hour
    }

    private val helperMessagesFile = File("helper-messages.json")
    private var helperMessagesLastFetch = Instant.now().minusSeconds(UPDATE_INTERVAL_IN_SECONDS + 1)

    fun checkForUpdate() {
        val currentTime = Instant.now()

        val secondsSinceLastUpdate =
            Duration.between(helperMessagesLastFetch, currentTime).abs().toSeconds()

        // Update helper messages if necessary
        if (secondsSinceLastUpdate >= UPDATE_INTERVAL_IN_SECONDS) {
            helperMessageService.updateHelperMessages(helperMessagesFile)

            helperMessagesLastFetch = currentTime
        }
    }
}
