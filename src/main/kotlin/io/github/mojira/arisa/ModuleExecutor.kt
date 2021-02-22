package io.github.mojira.arisa

import arrow.core.Either
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.applyIssueChanges
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import java.time.Instant

class ModuleExecutor(
    private val config: Config,
    private val registry: ModuleRegistry,
    private val queryCache: Cache<List<Issue>>,
    private val issueUpdateContextCache: IssueUpdateContextCache,
    private val searchIssues:
        (Cache<MutableSet<String>>, Cache<MutableSet<String>>, String, Int, () -> Unit) -> List<Issue>
) {
    private var postedCommentCache = Cache<MutableSet<String>>()

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
        val newPostedCommentCache = Cache<MutableSet<String>>()

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
                        exec.partially2(lastRun),
                        newPostedCommentCache
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
            postedCommentCache = newPostedCommentCache
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
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>,
        newPostedCommentCache: Cache<MutableSet<String>>
    ) {
        getIssues(
            moduleConfig, rerunTickets, moduleJql, queryCache, startAt, onQueryNotAtResultEnd, newPostedCommentCache
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
                issueUpdateContextCache.storage.forEach {
                    it.value.triggeredBy = it.value.triggeredBy ?: issue
                }
            }

        issueUpdateContextCache.storage
            .mapValues { Pair(it.value.triggeredBy, applyIssueChanges(it.value)) }
            .filterValues { (_, result) -> result.isLeft() }
            .forEach { (updateTo, pair) ->
                val (triggeredBy, result) = pair
                (result as Either.Left).a.exceptions.forEach {
                    log.error("[UPDATE] [TO $updateTo] [BY $triggeredBy] Failed", it)
                }
                addFailedTicket(triggeredBy!!)
            }
        issueUpdateContextCache.clear()
    }

    @Suppress("LongParameterList")
    fun getIssues(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        rerunTickets: Collection<String>,
        moduleJql: String,
        queryCache: Cache<List<Issue>>,
        startAt: Int,
        onQueryNotAtResultEnd: () -> Unit,
        newPostedCommentCache: Cache<MutableSet<String>>
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
            postedCommentCache,
            newPostedCommentCache,
            jql,
            startAt,
            onQueryNotAtResultEnd
        )

        queryCache.add(jql, issues)

        log.debug("Returned issues for module ${ moduleConfig::class.simpleName }: ${ issues.map { it.key } }")

        return issues
            .filter { it.project.key in projects }
            .filter { it.status.toLowerCase() !in excludedStatuses }
            .filter { it.resolution?.toLowerCase() ?: "unresolved" in resolutions }
    }
}
