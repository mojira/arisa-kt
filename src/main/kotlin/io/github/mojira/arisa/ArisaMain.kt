package io.github.mojira.arisa

import io.github.mojira.arisa.infrastructure.apiclient.JiraClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

/**
 * Global instance for the Jira client.
 * (not nice but it works)
 *
 * Gets initialized by [JiraConnectionService.connect]
 */
lateinit var jiraClient: JiraClient

fun main() {
    val configService = ConfigService()

    val webhookService = WebhookService(configService.config)
    webhookService.setLoggerWebhooks()

    val connectionService = JiraConnectionService(configService.config)
    connectionService.connect()

    val executionService = ExecutionService(configService.config, connectionService)

    while (true) {
        val secondsToSleep = executionService.runExecutionCycle()
        TimeUnit.SECONDS.sleep(secondsToSleep)
    }
}
