package io.github.mojira.arisa

import net.rcarz.jiraclient.JiraClient
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

val configService = ConfigService()
val webhookService = WebhookService(configService.config)
val connectionService = JiraConnectionService(configService.config)
val executionService = ExecutionService(configService.config, connectionService)

fun main() {
    webhookService.setLoggerWebhooks()

    connectionService.connect()

    while (true) {
        val secondsToSleep = executionService.runExecutionCycle()
        TimeUnit.SECONDS.sleep(secondsToSleep)
    }
}
