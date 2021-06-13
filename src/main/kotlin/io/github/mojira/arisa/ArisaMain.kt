package io.github.mojira.arisa

import arrow.syntax.function.partially1
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import com.github.napstr.logback.DiscordAppender
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.HelperMessageService
import io.github.mojira.arisa.infrastructure.ProjectCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.connectToJira
import io.github.mojira.arisa.infrastructure.jira.toDomain
import io.github.mojira.arisa.registry.TicketQueryTimeframe
import io.github.mojira.arisa.registry.getModuleRegistries
import net.rcarz.jiraclient.JiraClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Long.max
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

private const val TIME_MINUTES = 5L
private const val MAX_QUERY_TIMEFRAME_IN_MINUTES = 10L

const val MAX_RESULTS = 50

private const val RELOG_INTERVAL_IN_SECONDS = 5 * 60L
private const val WAIT_TIME_AFTER_CONNECTION_ERROR_IN_SECONDS = 40L

lateinit var jiraClient: JiraClient

@Suppress("LongMethod", "TooGenericExceptionCaught", "NestedBlockDepth")
fun main() {
    val config = readConfig()
    setWebhookOfLogger(config)

    jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )
    log.info("Connected to jira")

    // Get tickets for re-run and last run time
    var lastRelog = Instant.now()
    val lastRunFile = File("last-run")
    val lastRun = readLastRun(lastRunFile)
    var lastRunTime = readLastRunTime(lastRun)
    var rerunTickets = lastRun.subList(1, lastRun.size).toSet()
    val failedTickets = mutableSetOf<String>()

    val checkIntervalSeconds = config[Arisa.Issues.checkIntervalSeconds]

    // Read helper-messages
    val helperMessagesFile = File("helper-messages.json")
    val helperMessagesInterval = config[Arisa.HelperMessages.updateIntervalSeconds]
    HelperMessageService.updateHelperMessages(helperMessagesFile)
    var helperMessagesLastFetch = Instant.now()

    // Create executor
    val moduleRegistries = getModuleRegistries(config)
    var executor = Executor(
        config, moduleRegistries, getSearchIssues(jiraClient, config)
    )

    while (true) {
        // Save time before run, so nothing happening during the run is missed
        val currentTime = Instant.now()
        val endOfMaxTimeframe = lastRunTime.plus(MAX_QUERY_TIMEFRAME_IN_MINUTES, ChronoUnit.MINUTES)

        // The time at which we execute the bot.
        // If we exceed our maximum time frame, act as if we were executing at the end of the maximum time frame.
        // This is to make sure that `last-run` is updated once in a while,
        // even if the bot is catching up after a long downtime.
        val runOpenEnded = currentTime.isBefore(endOfMaxTimeframe)
        val currentRunTime = if (runOpenEnded) { currentTime } else { endOfMaxTimeframe }

        val timeframe = TicketQueryTimeframe(lastRunTime, currentRunTime, runOpenEnded)

        // Execute all enabled modules using the executor
        val executionResults = executor.execute(timeframe, rerunTickets)

        if (executionResults.successful) {
            rerunTickets = emptySet()
            failedTickets.addAll(executionResults.failedTickets)
            val failed = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

            if (config[Arisa.Debug.updateLastRun]) {
                lastRunFile.writeText("${currentRunTime.toEpochMilli()}$failed")
            }
            lastRunTime = currentRunTime
            TimeUnit.SECONDS.sleep(checkIntervalSeconds)
        } else {
            if (Duration.between(lastRelog, Instant.now()).toMinutes() >= 1) {
                // If last relog was more than a minute before and execution failed with an exception, relog
                log.info("Trying to relog")
                try {
                    jiraClient =
                        connectToJira(
                            config[Arisa.Credentials.username],
                            config[Arisa.Credentials.password],
                            config[Arisa.Issues.url]
                        )
                    log.info("Relog was successful")
                    lastRelog = Instant.now()
                    executor = Executor(
                        config, moduleRegistries, getSearchIssues(jiraClient, config)
                    )
                    TimeUnit.SECONDS.sleep(checkIntervalSeconds)
                } catch (exception: Exception) {
                    log.error("Relog failed", exception)
                    // Wait before performing any further action
                    TimeUnit.SECONDS.sleep(max(RELOG_INTERVAL_IN_SECONDS, checkIntervalSeconds))
                }
            } else {
                // Not enough time for relog passed, just try waiting a bit
                log.info("Not enough time for relog has passed")
                TimeUnit.SECONDS.sleep(max(WAIT_TIME_AFTER_CONNECTION_ERROR_IN_SECONDS, checkIntervalSeconds))
            }
        }

        // Update helper messages if necessary
        if (currentTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
            HelperMessageService.updateHelperMessages(helperMessagesFile)
            executor = Executor(
                config, moduleRegistries, getSearchIssues(jiraClient, config)
            )
            helperMessagesLastFetch = currentTime
        }
    }
}

private fun getSearchIssues(
    jiraClient: JiraClient,
    config: Config
): (String, Int, () -> Unit) -> List<Issue> {
    return ::searchIssues
        .partially1(jiraClient)
        .partially1(config)
}

private fun readLastRunTime(lastRun: List<String>): Instant {
    return if (lastRun[0].isNotEmpty())
        Instant.ofEpochMilli(lastRun[0].toLong())
    else Instant.now().minus(TIME_MINUTES, ChronoUnit.MINUTES)
}

private fun readLastRun(lastRunFile: File): List<String> {
    return (if (lastRunFile.exists())
        lastRunFile.readText().trim()
    else "")
        .split(",")
}

private fun readConfig(): Config {
    return Config { addSpec(Arisa) }
        .from.yaml.watchFile("config/config.yml")
        .from.yaml.watchFile("config/local.yml")
        .from.env()
        .from.systemProperties()
}

private fun setWebhookOfLogger(config: Config) {
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

@Suppress("LongParameterList")
private fun searchIssues(
    jiraClient: JiraClient,
    config: Config,
    jql: String,
    startAt: Int,
    finishedCallback: () -> Unit
): List<Issue> {
    val searchResult = jiraClient.searchIssues(
        jql,
        "*all",
        "changelog",
        MAX_RESULTS,
        startAt
    ) ?: return emptyList()

    if (startAt + searchResult.max >= searchResult.total) finishedCallback()

    return searchResult
        .issues
        .map {
            it.toDomain(
                jiraClient,
                ProjectCache.getProjectFromTicketId(it.key),
                config
            )
        }
}
