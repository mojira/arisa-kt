package io.github.mojira.arisa

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import com.github.napstr.logback.DiscordAppender
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.getHelperMessages
import io.github.mojira.arisa.infrastructure.jira.JiraIssueService
import io.github.mojira.arisa.infrastructure.jira.JiraUserService
import io.github.mojira.arisa.infrastructure.jira.MapFromJira
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.TokenCredentials
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

const val TIME_MINUTES = 5L
const val MAX_RESULTS = 50
lateinit var jiraClient: JiraClient
lateinit var credentials: TokenCredentials

@Suppress("LongMethod")
fun main() {
    val config = readConfig()
    setWebhookOfLogger(config)

    credentials = TokenCredentials(config[Arisa.Credentials.username], config[Arisa.Credentials.password])
    jiraClient = connectToJira(credentials, config[Arisa.Issues.url])
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
    var helperMessages = helperMessagesFile.getHelperMessages()
    var helperMessagesLastFetch = Instant.now()

    // Initialize caches and registry
    val moduleRegistry = ModuleRegistry(config)

    val enabledModules = moduleRegistry.getEnabledModules().map { it.name }
    log.debug("Enabled modules: $enabledModules")

    // Create module executor
    val jiraUserService = JiraUserService(jiraClient)
    val jiraIssueService = JiraIssueService(jiraClient, config, MapFromJira(config, jiraUserService))
    var moduleExecutor = ModuleExecutor(config, moduleRegistry, jiraIssueService)

    while (true) {
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now()
        val executionResults = moduleExecutor.execute(lastRunTime, rerunTickets)

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
            jiraClient = connectToJira(credentials, config[Arisa.Issues.url])
            moduleExecutor = ModuleExecutor(config, moduleRegistry, jiraIssueService)
        }

        if (curRunTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
            helperMessages = helperMessagesFile.getHelperMessages(helperMessages)
            moduleExecutor = ModuleExecutor(config, moduleRegistry, jiraIssueService)
            helperMessagesLastFetch = curRunTime
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkIntervalSeconds])
    }
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

private fun connectToJira(tokenCredentials: TokenCredentials, url: String) = JiraClient(url, credentials)