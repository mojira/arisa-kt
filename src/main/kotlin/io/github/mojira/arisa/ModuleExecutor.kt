package io.github.mojira.arisa

import arrow.core.Either
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.HelperMessages
import io.github.mojira.arisa.infrastructure.QueryCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.toDomain
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import net.rcarz.jiraclient.JiraClient
import java.time.Instant
import net.rcarz.jiraclient.Issue as JiraIssue

private const val MAX_RESULTS = 50

class ModuleExecutor(
    private val jiraClient: JiraClient,
    private val config: Config,
    private val queryCache: QueryCache,
    private val helperMessages: HelperMessages
) {
    private val registry = ModuleRegistry(config)

    data class ExecutionResults(
        val successful: Boolean,
        val failedTickets: Collection<String>
    )

    @Suppress("TooGenericExceptionCaught")
    fun execute(lastRun: Instant, rerunTickets: Set<String>): ExecutionResults {
        val failedTickets = mutableSetOf<String>()

        try {
            var missingResultsPage: Boolean
            var startAt = 0

            do {
                missingResultsPage = false

                registry.getModules().forEach { (_, config, getJql, exec) ->
                    executeModule(
                        config,
                        queryCache,
                        rerunTickets,
                        getJql(lastRun),
                        startAt,
                        failedTickets::add,
                        { missingResultsPage = true },
                        exec.partially2(lastRun)
                    )
                }

                queryCache.clear()
                startAt += MAX_RESULTS
            } while (missingResultsPage)
            return ExecutionResults(true, failedTickets)
        } catch (ex: Throwable) {
            log.error("Failed to execute modules", ex)
            return ExecutionResults(false, failedTickets)
        }
    }

    @Suppress("LongParameterList")
    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        queryCache: QueryCache,
        rerunTickets: Collection<String>,
        moduleJql: String,
        startAt: Int,
        addFailedTicket: (String) -> Any,
        onQueryNotAtResultEnd: () -> Unit,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        val projects = (config[moduleConfig.whitelist] ?: config[Arisa.Issues.projects])
        val resolutions = config[moduleConfig.resolutions].map(String::toLowerCase)
        val excludedStatuses = config[moduleConfig.excludedStatuses].map(String::toLowerCase)
        val failedTicketsJQL = with(rerunTickets) {
            if (isNotEmpty())
                "key in (${joinToString(",")}) OR "
            else ""
        }

        val jql = "$failedTicketsJQL($moduleJql)"
        val issues = queryCache.get(jql) ?: searchIssues(jql, startAt, onQueryNotAtResultEnd)
            .map { it.toDomain(jiraClient, jiraClient.getProject(it.project.key), helperMessages, config) }

        queryCache.add(jql, issues)

        issues
            .filter { it.project.key in projects }
            .filter { it.status.toLowerCase() !in excludedStatuses }
            .filter { it.resolution?.toLowerCase() ?: "unresolved" in resolutions }
            .map { it.key to executeModule(it) }
            .forEach { (issue, response) ->
                response.second.fold({
                    when (it) {
                        is OperationNotNeededModuleResponse -> if (config[Arisa.logOperationNotNeeded]) {
                            log.info("[RESPONSE] [$issue] [${response.first}] Operation not needed")
                        }
                        is FailedModuleResponse -> {
                            addFailedTicket(issue)

                            for (exception in it.exceptions) {
                                log.error("[RESPONSE] [$issue] [${response.first}] Failed", exception)
                            }
                        }
                    }
                }, {
                    log.info("[RESPONSE] [$issue] [${response.first}] Successful")
                })
            }
    }

    private fun searchIssues(
        jql: String,
        startAt: Int,
        onQueryPaginated: () -> Unit
    ): List<JiraIssue> {
        val searchResult = jiraClient
            .searchIssues(jql, "*all", "changelog", MAX_RESULTS, startAt)

        if (startAt + searchResult.max < searchResult.total)
            onQueryPaginated()

        return searchResult
            .issues
    }
}
