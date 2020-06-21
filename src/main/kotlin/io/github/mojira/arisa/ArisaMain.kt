package io.github.mojira.arisa

import arrow.syntax.function.partially1
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

    var jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )
    var lastRelog = Instant.now()

    log.info("Connected to jira")

    val lastRunFile = File("last-run")
    val lastRun = readLastRun(lastRunFile)

    var lastRunTime = readLastRunTime(lastRun)

    var rerunTickets = lastRun.subList(1, lastRun.size).toSet()
    val failedTickets = mutableSetOf<String>()

    val queryCache = Cache<List<Issue>>()
    val issueUpdateContextCache = Cache<IssueUpdateContext>()

    val helperMessagesFile = File("helper-messages.json")
    val helperMessagesInterval = config[Arisa.HelperMessages.updateIntervalSeconds]
    var helperMessages = helperMessagesFile.getHelperMessages()
    var helperMessagesLastFetch = Instant.now()

    var moduleExecutor = ModuleExecutor(config, queryCache, issueUpdateContextCache)

    while (true) {
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now()
        val searchIssues = ::searchIssues
            .partially1(jiraClient)
            .partially1(helperMessages)
            .partially1(config)
            .partially1(issueUpdateContextCache)
        val executionResults = moduleExecutor.execute(lastRunTime, rerunTickets, searchIssues)

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
            moduleExecutor = ModuleExecutor(config, queryCache, issueUpdateContextCache)
        }

        if (curRunTime.epochSecond - helperMessagesLastFetch.epochSecond >= helperMessagesInterval) {
            helperMessages = helperMessagesFile.getHelperMessages(helperMessages)
            moduleExecutor = ModuleExecutor(config, queryCache, issueUpdateContextCache)
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
        lastRunFile.readText()
    else "")
        .split(",")
}

private fun readConfig(): Config {
    return Config { addSpec(Arisa) }
        .from.yaml.watchFile("arisa.yml")
        .from.json.watchFile("arisa.json")
        .from.env()
        .from.systemProperties()
}

@Suppress("LongParameterList")
private fun searchIssues(
    jiraClient: JiraClient,
    helperMessages: HelperMessages,
    config: Config,
    issueUpdateContextCache: IssueUpdateContextCache,
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
                issueUpdateContextCache
            )
        }
}
