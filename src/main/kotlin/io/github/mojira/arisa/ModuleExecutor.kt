package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.service.IssueService
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.services.Cache
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import java.time.Instant

class ModuleExecutor(
    private val config: Config,
    private val registry: ModuleRegistry,
    private val issueService: IssueService
) {
    private val DEFAULT_JQL = { lastRun: Instant -> "updated > ${lastRun.toEpochMilli()}" }

    private var postedCommentCache = Cache<MutableSet<String>>()

    data class ExecutionResults(
        val successful: Boolean,
        val failedTickets: Collection<String>
    )

    @Suppress("TooGenericExceptionCaught")
    fun execute(
        lastRun: Instant,
        rerunTickets: Set<String>,
    ): ExecutionResults {
        val failedTickets = mutableSetOf<String>()
        val newPostedCommentCache = Cache<MutableSet<String>>()

        try {
            var missingResultsPage: Boolean
            var startAt = 0

            do {
                missingResultsPage = false

                val issues = issueService
                    .searchIssues(getQuery(rerunTickets, lastRun), startAt)
                    .map { it.key to it }
                    .toMap()
                    .toMutableMap()
                registry.getEnabledModules().forEach { (_, moduleConfig, exec) ->
                    executeModule(
                        issues.values.filterForModule(moduleConfig),
                        failedTickets::add,
                        exec,
                    ).forEach { issues[it.key] = it }
                }

                startAt += MAX_RESULTS
            } while (missingResultsPage)
            return ExecutionResults(true, failedTickets)
        } catch (ex: Throwable) {
            log.error("Failed to execute modules", ex)
            return ExecutionResults(false, failedTickets)
        } finally {
            postedCommentCache = newPostedCommentCache
        }
    }

    private fun getQuery(rerunTickets: Set<String>, lastRun: Instant): String {
        var query = DEFAULT_JQL(lastRun)
        if (rerunTickets.isNotEmpty()) {
            query + " OR key in (${rerunTickets.joinToString(",")})"
        }
        return query
    }

    private fun Collection<Issue>.filterForModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec
    ) = this
        .filter { it.project.key in config[moduleConfig.projects] ?: config[Arisa.Issues.projects] }
        .filter { it.status.toLowerCase() !in config[moduleConfig.excludedStatuses].map(String::toLowerCase) }
        .filter {
            it.resolution?.toLowerCase() ?: "unresolved" in (config[moduleConfig.resolutions]
                ?: config[Arisa.Issues.resolutions])
                .map(String::toLowerCase)
        }

    @Suppress("LongParameterList")
    private fun executeModule(
        issues: List<Issue>,
        addFailedTicket: (String) -> Unit,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>,
    ): List<Issue> {
        val responseIssues = mutableListOf<Issue>()
        issues
            .map { executeModule(it) }
            .forEach { (module, response) ->
                response.fold({
                    responseIssues.add(it.issue)
                    when (it) {
                        is OperationNotNeededModuleResponse -> if (config[Arisa.Debug.logOperationNotNeeded]) {
                            log.debug("[RESPONSE] [$module] [${it.issue.key}] Operation not needed")
                        }
                        is FailedModuleResponse -> {
                            addFailedTicket(it.issue.key)

                            for (exception in it.exceptions) {
                                log.error("[RESPONSE] [$module] [${it.issue.key}] Failed", exception)
                            }
                        }
                    }
                }, {
                    responseIssues.add(it.issue)
                    log.info("[RESPONSE] [$module] [${it.issue.key}] Successful")
                })
            }
        return responseIssues
    }
}
