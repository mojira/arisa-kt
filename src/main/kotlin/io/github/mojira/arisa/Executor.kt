package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.CommentCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.registry.ModuleRegistry

class Executor(
    private val config: Config,
    private val registries: List<ModuleRegistry>,
    private val issueFetcher: IssueFetcher
) {
    data class ExecutionResults(
        val successful: Boolean,
        val failedTickets: Set<String>
    )

    @Suppress("TooGenericExceptionCaught")
    fun execute(
        timeframe: ExecutionTimeframe,
        rerunTickets: Set<String>
    ): ExecutionResults {
        val failedTickets = mutableSetOf<String>()

        log.debug("Executing timeframe $timeframe")

        try {
            registries.forEach {
                executeRegistry(it, rerunTickets, timeframe) { ticket ->
                    if (ticket !in rerunTickets) {
                        failedTickets.add(ticket)
                    } else {
                        log.info("$ticket failed to run again, dropping it.")
                    }
                }
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
        timeframe: ExecutionTimeframe,
        addFailedTicket: (String) -> Unit
    ) {
        val issues = getIssuesForRegistry(registry, rerunTickets, timeframe)

        registry.getEnabledModules().forEach { (moduleName, _, execute, moduleExecutor) ->
            log.debug("Executing module $moduleName")
            moduleExecutor.executeModule(issues, addFailedTicket) { issue -> execute(issue, timeframe.lastRunTime) }
        }
    }

    private fun getIssuesForRegistry(
        registry: ModuleRegistry,
        rerunTickets: Collection<String>,
        timeframe: ExecutionTimeframe
    ): List<Issue> {
        val jql = registry.getFullJql(timeframe, rerunTickets)

        if (config[Arisa.Debug.logQueryJql]) {
            log.debug("${registry::class.simpleName} JQL: `$jql`")
        }

        val issues = issueFetcher.fetchAllIssues(jql)

        if (config[Arisa.Debug.logReturnedIssues]) {
            log.debug("Returned issues for registry ${registry::class.simpleName}: ${issues.map { it.key }}")
        } else {
            log.debug("${issues.size} issues have been returned for registry ${registry::class.simpleName}")
        }

        return issues
    }

    init {
        val enabledModules = registries.flatMap { registry -> registry.getEnabledModules().map { it.name } }
        log.debug("Enabled modules: $enabledModules")
    }
}
