package io.github.mojira.arisa

import io.github.mojira.arisa.domain.service.CommentCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.JiraIssueService
import io.github.mojira.arisa.infrastructure.jira.JiraUserService
import io.github.mojira.arisa.infrastructure.jira.MapFromJira
import io.github.mojira.arisa.infrastructure.jira.MapToJira
import io.github.mojira.arisa.infrastructure.services.ConfigService
import io.github.mojira.arisa.infrastructure.services.HelperMessageService
import io.github.mojira.arisa.infrastructure.services.LastRunService
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

val config = ConfigService.readConfig()

//last run
val lastRelog = Instant.now()
val lastRun = LastRunService.readLastRun()
var lastRunTime = LastRunService.readLastRunTime(lastRun)
var rerunTickets = lastRun.subList(1, lastRun.size).toSet()
val failedTickets = mutableSetOf<String>()

// helper messages
val helperMessagesFile = File("helper-messages.json")
val helperMessagesInterval = config[Arisa.HelperMessages.updateIntervalSeconds]
var helperMessagesLastFetch = Instant.now()

// modules
val moduleRegistry = ModuleRegistry(config)
val enabledModules = moduleRegistry.getEnabledModules().map { it.name }

val commentCache = CommentCache()
val userService = JiraUserService(jiraClient)
val issueService =
    JiraIssueService(jiraClient, config, MapToJira(config, commentCache), MapFromJira(config, userService))
var moduleExecutor = ModuleExecutor(config, moduleRegistry, issueService)

fun main() {
    ConfigService.setWebhookOfLogger(config)

    credentials = TokenCredentials(config[Arisa.Credentials.username], config[Arisa.Credentials.password])
    jiraClient = JiraClient(config[Arisa.Issues.url], credentials)
    log.info("Connected to jira")

    HelperMessageService.updateHelperMessages(helperMessagesFile)
    log.debug("Enabled modules: $enabledModules")

    while (true) {
        mainLoop()
    }
}

private fun mainLoop() {
    // save time before run, so nothing happening during the run is missed
    val curRunTime = Instant.now()
    val executionResults = moduleExecutor.execute(lastRunTime, rerunTickets)

    if (executionResults.successful) {
        rerunTickets = emptySet()
        failedTickets.addAll(executionResults.failedTickets)
        val failed = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

        if (config[Arisa.Debug.updateLastRun]) {
            LastRunService.writeLastRun(curRunTime, failed)
        }
        lastRunTime = curRunTime
    } else if (lastRelog.plus(1, ChronoUnit.MINUTES).isAfter(Instant.now())) {
        // If last relog was more than a minute before and execution failed with an exception, relog
        jiraClient = JiraClient(config[Arisa.Issues.url], credentials)
        moduleExecutor = ModuleExecutor(config, moduleRegistry, issueService)
    }

    if (curRunTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
        HelperMessageService.updateHelperMessages(helperMessagesFile)
        moduleExecutor = ModuleExecutor(config, moduleRegistry, issueService)
        helperMessagesLastFetch = curRunTime
    }

    issueService.cleanup()
    TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkIntervalSeconds])
}