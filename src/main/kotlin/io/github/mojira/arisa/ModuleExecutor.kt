package io.github.mojira.arisa

import arrow.core.Either
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.CommentCache
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import java.time.Instant

class ModuleExecutor(
    private val config: Config,
    private val registry: ModuleRegistry,
    private val queryCache: Cache<List<Issue>>,
    private val searchIssues: (String, Int, () -> Unit) -> List<Issue>
) {
    data class ExecutionResults(
        val successful: Boolean,
        val failedTickets: Collection<String>
    )

    @Suppress("TooGenericExceptionCaught")
    fun execute(
        lastRun: Instant,
        rerunTickets: Set<String>
    ): ExecutionResults {
        val failedTickets = mutableSetOf<String>()

        try {
            var missingResultsPage: Boolean
            var startAt = 0

            do {
                missingResultsPage = false

                registry.getEnabledModules().forEach { (_, config, getJql, exec) ->
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
        } finally {
            CommentCache.flush()
        }
    }

    @Suppress("LongParameterList")
    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        queryCache: Cache<List<Issue>>,
        rerunTickets: Collection<String>,
        moduleJql: String,
        startAt: Int,
        addFailedTicket: (String) -> Any,
        onQueryNotAtResultEnd: () -> Unit,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        getIssues(
            moduleConfig, rerunTickets, moduleJql, queryCache, startAt, onQueryNotAtResultEnd
        )
            .map { it.key to executeModule(it) }
            .forEach { (issue, response) ->
                response.second.fold({
                    when (it) {
                        is OperationNotNeededModuleResponse -> if (config[Arisa.Debug.logOperationNotNeeded]) {
                            log.debug("[RESPONSE] [$issue] [${response.first}] Operation not needed")
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
                IssueUpdateContextCache.updateTriggeredBy(issue)
            }

        IssueUpdateContextCache.applyChanges(addFailedTicket)
    }

    @Suppress("LongParameterList")
    fun getIssues(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        rerunTickets: Collection<String>,
        moduleJql: String,
        queryCache: Cache<List<Issue>>,
        startAt: Int,
        onQueryNotAtResultEnd: () -> Unit
    ): List<Issue> {
        val projects = config[moduleConfig.projects] ?: config[Arisa.Issues.projects]

        val resolutions = (config[moduleConfig.resolutions] ?: config[Arisa.Issues.resolutions])
            .map(String::toLowerCase)

        val excludedStatuses = config[moduleConfig.excludedStatuses].map(String::toLowerCase)

        val failedTicketsJQL = with(rerunTickets) {
            if (isNotEmpty())
                "key in (${joinToString(",")}) OR "
            else ""
        }

        val jql = "$failedTicketsJQL($moduleJql)"
        val issues = queryCache.get(jql) ?: searchIssues(
            jql,
            startAt,
            onQueryNotAtResultEnd
        )

        queryCache.add(jql, issues)

        if (config[Arisa.Debug.logReturnedIssues]) {
            log.debug("Returned issues for module ${ moduleConfig::class.simpleName }: ${ issues.map { it.key } }")
        }

        return issues
            .filter { it.project.key in projects }
            .filter { it.status.toLowerCase() !in excludedStatuses }
            .filter { it.resolution?.toLowerCase() ?: "unresolved" in resolutions }
    }
}
