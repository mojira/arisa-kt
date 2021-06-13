package io.github.mojira.arisa

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import com.github.napstr.logback.DiscordAppender
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.rcarz.jiraclient.JiraClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

lateinit var jiraClient: JiraClient

private val config = readConfig()
private val connectionService = JiraConnectionService(config)
private var executor = Executor(config)

const val MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS = 10L

fun main() {
    setLoggerWebhooks()

    connectionService.connect()

    while (true) {
        HelperMessageUpdateService.checkForUpdate()

        val secondsToSleep = runExecutionCycle(executor)
        TimeUnit.SECONDS.sleep(secondsToSleep)
    }
}

/**
 * @return amount of seconds to sleep after this execution cycle
 */
private fun runExecutionCycle(executor: Executor): Long {
    val timeframe = ExecutionTimeframe.getTimeframeFromLastRun()
    val currentRunTime = timeframe.currentRunTime

    // Execute all enabled modules using the executor
    val executionResults = executor.execute(timeframe, LastRun.failedTickets)

    return if (executionResults.successful) {
        // Reset relog timer
        connectionService.notifyOfSuccessfulConnection()

        // Update last run and save it to file
        LastRun.update(currentRunTime, executionResults.failedTickets)
        if (config[Arisa.Debug.updateLastRun]) {
            LastRun.writeToFile()
        }

        MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS
    } else {
        connectionService.tryRelog().sleepTimeInSeconds
    }
}

/**
 * Reads the config from the config file(s) and other sources
 */
private fun readConfig(): Config {
    return Config { addSpec(Arisa) }
        .from.yaml.watchFile("config/config.yml")
        .from.yaml.watchFile("config/local.yml")
        .from.env()
        .from.systemProperties()
}

/**
 * Configures the logger, so that it sends log messages to the Discord webhooks
 */
private fun setLoggerWebhooks() {
    val context = LoggerFactory.getILoggerFactory() as LoggerContext

    val discordAsync = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD") as AsyncAppender?
    if (discordAsync != null) {
        val discordAppender = discordAsync.getAppender("DISCORD") as DiscordAppender
        discordAppender.webhookUri = config[Arisa.Credentials.discordLogWebhook]
    }

    val discordErrorAsync =
        context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_ERROR_DISCORD") as AsyncAppender?
    if (discordErrorAsync != null) {
        val discordErrorAppender = discordErrorAsync.getAppender("ERROR_DISCORD") as DiscordAppender
        discordErrorAppender.webhookUri = config[Arisa.Credentials.discordErrorLogWebhook]
    }
}
