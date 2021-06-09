package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.CommentCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.registry.ModuleRegistry
import java.time.Instant

class Executor(
    private val config: Config,
    private val registries: List<ModuleRegistry>,
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
            registries.forEach {
                executeRegistry(it, rerunTickets, failedTickets::add, lastRun)
            }
        } catch (ex: Throwable) {
            log.error("Failed to execute modules", ex)
            return ExecutionResults(false, failedTickets)
        } finally {
            CommentCache.flush()
        }

        return ExecutionResults(true, failedTickets)
    }

    private fun executeRegistry(
        registry: ModuleRegistry,
        rerunTickets: Collection<String>,
        addFailedTicket: (String) -> Unit,
        lastRun: Instant
    ) {
        val issues = getIssuesForRegistry(registry, rerunTickets, lastRun)

        registry.getEnabledModules().forEach { (moduleName, _, execute, moduleExecutor) ->
            log.debug("Executing module $moduleName")
            moduleExecutor.executeModule(issues, addFailedTicket) { issue -> execute(issue, lastRun) }
        }
    }

    private fun getIssuesForRegistry(
        registry: ModuleRegistry,
        rerunTickets: Collection<String>,
        lastRun: Instant
    ): List<Issue> {
        val issues = mutableListOf<Issue>()

        val jql = registry.getFullJql(lastRun, rerunTickets)

        var continueSearching = true
        var startAt = 0

        while (continueSearching) {
            val searchResult = searchIssues(
                jql,
                startAt
            ) { continueSearching = false }

            issues.addAll(searchResult)

            startAt += MAX_RESULTS
        }

        if (config[Arisa.Debug.logReturnedIssues]) {
            log.debug("Returned issues for registry ${ registry::class.simpleName }: ${ issues.map { it.key } }")
        } else {
            log.debug("${ issues.size } issues have been returned for registry ${ registry::class.simpleName }")
        }

        return issues
    }

    init {
        val enabledModules = registries.flatMap { smth -> smth.getEnabledModules().map { it.name } }
        log.debug("Enabled modules: $enabledModules")
    }
}
