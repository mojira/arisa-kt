package io.github.mojira.arisa

import arrow.syntax.function.partially1
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import com.github.napstr.logback.DiscordAppender
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.HelperMessages
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.getHelperMessages
import io.github.mojira.arisa.infrastructure.jira.connectToJira
import io.github.mojira.arisa.infrastructure.jira.toDomain
import net.rcarz.jiraclient.JiraClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Arisa")

const val TIME_MINUTES = 5L
const val MAX_RESULTS = 50

fun main() {
    val config = readConfig()
    setWebhookOfLogger(config)

    var jiraClient =
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
    var helperMessages = helperMessagesFile.getHelperMessages()
    var helperMessagesLastFetch = Instant.now()

    // Initialize caches and registry
    val queryCache = Cache<List<Issue>>()
    val issueUpdateContextCache = Cache<IssueUpdateContext>()
    val moduleRegistry = ModuleRegistry(config)

    // Create module executor
    var moduleExecutor = ModuleExecutor(
        config, moduleRegistry, queryCache, issueUpdateContextCache,
        getSearchIssues(jiraClient, helperMessages, config, issueUpdateContextCache)
    )

    while (true) {
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now()
        val executionResults = moduleExecutor.execute(lastRunTime, rerunTickets)

        if (executionResults.successful) {
            rerunTickets = emptySet()
            failedTickets.addAll(executionResults.failedTickets)
            val failed = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

            lastRunFile.writeText("${curRunTime.toEpochMilli()}$failed")
            lastRunTime = curRunTime
        } else if (lastRelog.plus(1, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            // If last relog was more than a minute before and execution failed with an exception, relog
            jiraClient = connectToJira(
                config[Arisa.Credentials.username],
                config[Arisa.Credentials.password],
                config[Arisa.Issues.url]
            )
            moduleExecutor = ModuleExecutor(
                config, moduleRegistry, queryCache, issueUpdateContextCache,
                getSearchIssues(jiraClient, helperMessages, config, issueUpdateContextCache)
            )
        }

        if (curRunTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
            helperMessages = helperMessagesFile.getHelperMessages(helperMessages)
            moduleExecutor = ModuleExecutor(
                config, moduleRegistry, queryCache, issueUpdateContextCache,
                getSearchIssues(jiraClient, helperMessages, config, issueUpdateContextCache)
            )
            helperMessagesLastFetch = curRunTime
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkIntervalSeconds])
    }
}

private fun getSearchIssues(
    jiraClient: JiraClient,
    helperMessages: HelperMessages,
    config: Config,
    issueUpdateContextCache: Cache<IssueUpdateContext>
): (Cache<MutableSet<String>>, Cache<MutableSet<String>>, String, Int, () -> Unit) -> List<Issue> {
    return ::searchIssues
        .partially1(jiraClient)
        .partially1(helperMessages)
        .partially1(config)
        .partially1(issueUpdateContextCache)
}

private fun readLastRunTime(lastRun: List<String>): Instant {
    return if (lastRun[0].isNotEmpty())
        Instant.ofEpochMilli(lastRun[0].toLong())
    else Instant.now().minus(TIME_MINUTES, ChronoUnit.MINUTES)
}

private fun readLastRun(lastRunFile: File): List<String> {
    return (if (lastRunFile.exists())
        lastRunFile.readText()
    else "")
        .split(",")
}

private fun readConfig(): Config {
    return Config { addSpec(Arisa) }
        .from.yaml.watchFile("arisa.yml", optional = true)
        .from.json.watchFile("arisa.json")
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
}

@Suppress("LongParameterList")
private fun searchIssues(
    jiraClient: JiraClient,
    helperMessages: HelperMessages,
    config: Config,
    issueUpdateContextCache: IssueUpdateContextCache,
    oldPostedCommentCache: Cache<MutableSet<String>>,
    newPostedCommentCache: Cache<MutableSet<String>>,
    jql: String,
    startAt: Int,
    onQueryPaginated: () -> Unit
): List<Issue> {
    val searchResult = jiraClient
        .searchIssues(jql, "*all", "changelog", MAX_RESULTS, startAt)

    if (startAt + searchResult.max < searchResult.total)
        onQueryPaginated()

    return searchResult
        .issues
        .map {
            it.toDomain(
                jiraClient,
                jiraClient.getProject(it.project.key),
                helperMessages,
                config,
                issueUpdateContextCache,
                oldPostedCommentCache,
                newPostedCommentCache
            )
        }
}
