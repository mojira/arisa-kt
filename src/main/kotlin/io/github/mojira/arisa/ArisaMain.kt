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
import io.github.mojira.arisa.registry.getModuleRegistries
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

private const val TIME_MINUTES = 5L
const val MAX_RESULTS = 50
private const val MINUTES_FOR_THROTTLED_LOG = 30L
lateinit var jiraClient: JiraClient
private var throttledLog = 0L

@Suppress("LongMethod")
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
    val lastRelog = Instant.now()
    val lastRunFile = File("last-run")
    val lastRun = readLastRun(lastRunFile)
    var lastRunTime = readLastRunTime(lastRun)
    var rerunTickets = lastRun.subList(1, lastRun.size).toSet()
    val failedTickets = mutableSetOf<String>()

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
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now()
        val executionResults = executor.execute(lastRunTime, rerunTickets)

        if (executionResults.successful) {
            rerunTickets = emptySet()
            failedTickets.addAll(executionResults.failedTickets)
            val failed = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

            if (config[Arisa.Debug.updateLastRun]) {
                lastRunFile.writeText("${curRunTime.toEpochMilli()}$failed")
            }
            lastRunTime = curRunTime
        } else if (lastRelog.plus(1, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            // If last relog was more than a minute before and execution failed with an exception, relog
            jiraClient =
                connectToJira(
                    config[Arisa.Credentials.username],
                    config[Arisa.Credentials.password],
                    config[Arisa.Issues.url]
                )

            executor = Executor(
                config, moduleRegistries, getSearchIssues(jiraClient, config)
            )
        }

        if (curRunTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
            HelperMessageService.updateHelperMessages(helperMessagesFile)
            executor = Executor(
                config, moduleRegistries, getSearchIssues(jiraClient, config)
            )
            helperMessagesLastFetch = curRunTime
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkIntervalSeconds])
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
    val searchResult = try {
        jiraClient
            .searchIssues(jql, "*all", "changelog", MAX_RESULTS, startAt)
    } catch (e: JiraException) {
        if (System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(MINUTES_FOR_THROTTLED_LOG) > throttledLog) {
            log.warn("Failed to connect to jira. Caused by: ${e.cause?.message}")
            throttledLog = System.currentTimeMillis()
        }
        throw e
    } ?: return emptyList()

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
