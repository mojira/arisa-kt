package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.ModuleRegistry
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import net.rcarz.jiraclient.ChangeLogEntry
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient

private const val MAX_RESULTS = 50

class ModuleExecutor(
    private val jiraClient: JiraClient,
    private val config: Config,
    private val cache: Cache
) {
    private val registry = ModuleRegistry(jiraClient, config)

    fun execute(lastRun: Long): Boolean {
        try {
            var missingResultsPage: Boolean
            var startAt = 0

            do {
                missingResultsPage = false

                registry.getModules().forEach { (config, exec) ->
                    executeModule(
                        config,
                        cache,
                        lastRun,
                        startAt,
                        { missingResultsPage = true },
                        exec
                    )
                }

                cache.clearQueryCache()
                startAt += MAX_RESULTS
            } while (missingResultsPage)
            cache.updatedFailedTickets()

            return true
        } catch (ex: Throwable) {
            log.error("Failed to execute modules", ex)
            return false
        }
    }


    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        cache: Cache,
        lastRun: Long,
        startAt: Int,
        onQueryNotAtResultEnd: () -> Unit,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        val projects = (config[moduleConfig.whitelist] ?: config[Arisa.Issues.projects])
        val resolutions = config[moduleConfig.resolutions].map(String::toLowerCase)
        val failedTicketsJQL = with(cache.getFailedTickets()) {
            if (isNotEmpty())
                "key in (${joinToString(",")}) OR "
            else ""
        }

        val jql = "$failedTicketsJQL(${config[moduleConfig.jql].format(lastRun)})"
        val issues = cache.getQuery(jql) ?: searchIssues(jql, startAt, onQueryNotAtResultEnd)

        cache.addQuery(jql, issues)

        issues
            .filter { it.project.key in projects }
            .filter { it.resolution?.name?.toLowerCase() ?: "unresolved" in resolutions }
            .map { it.key to executeModule(it) }
            .forEach { (issue, response) ->
                response.second.fold({
                    when (it) {
                        is OperationNotNeededModuleResponse -> if (config[Arisa.logOperationNotNeeded]) log.info("[RESPONSE] [$issue] [${response.first}] Operation not needed")
                        is FailedModuleResponse -> {
                            cache.addFailedTicket(issue)

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
    ): List<Issue> {
        val searchResult = jiraClient
            .searchIssues(jql, "*all", "changelog", MAX_RESULTS, startAt)

        if (searchResult.start + searchResult.max < searchResult.total)
            onQueryPaginated()

        return searchResult
            .issues
            .filter(::lastActionWasNotAResolve)
    }

    private fun lastActionWasNotAResolve(issue: Issue): Boolean {
        val latestChange = issue.changeLog.entries.lastOrNull()

        return latestChange == null ||
                latestChange.isNotATransition() ||
                latestChange.wasDoneByTheBot() ||
                latestChange.commentAfterIt(issue)
    }

    private fun ChangeLogEntry.commentAfterIt(issue: Issue) =
        (issue.comments.isNotEmpty() && issue.comments.last().updatedDate > created)

    private fun ChangeLogEntry.wasDoneByTheBot() =
        author.name == config[Arisa.Credentials.username]

    private fun ChangeLogEntry.isNotATransition() =
        !items.any { it.field == "resolution" }

}
