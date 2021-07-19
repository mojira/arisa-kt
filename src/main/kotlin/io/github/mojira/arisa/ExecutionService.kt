package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.HelperMessageService
import io.github.mojira.arisa.infrastructure.ProjectCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.Mapper
import io.github.mojira.arisa.registry.getModuleRegistries

class ExecutionService(
    config: Config,
    private val connectionService: JiraConnectionService
) {
    private val helperMessageService = HelperMessageService(config[Arisa.botCommentSignatureMessage])
    private val helperMessageUpdateService = HelperMessageUpdateService(helperMessageService)

    private val executor: Executor
    private val lastRun = LastRun.getLastRun(config)

    init {
        val projectCache = ProjectCache(connectionService)
        val moduleRegistries = getModuleRegistries(config, connectionService, projectCache, helperMessageService)
        val mapper = Mapper(connectionService, config, projectCache, helperMessageService)
        val issueFetcher = IssueFetcher.createFetcher(connectionService, mapper)

        executor = Executor(
            config,
            moduleRegistries,
            issueFetcher
        )
    }

    /**
     * @return amount of seconds to sleep after this execution cycle
     */
    fun runExecutionCycle(): Long {
        helperMessageUpdateService.checkForUpdate()

        val timeframe = ExecutionTimeframe.getTimeframeFromLastRun(lastRun)
        val currentRunTime = timeframe.currentRunTime

        // Execute all enabled modules using the executor
        val executionResults = executor.execute(timeframe, lastRun.failedTickets)

        return if (executionResults.successful) {
            // Reset relog timer
            connectionService.notifyOfSuccessfulConnection()

            // Update last run and save it to file
            lastRun.update(currentRunTime, executionResults.failedTickets)

            JiraConnectionService.MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS
        } else {
            connectionService.tryRelog().sleepTimeInSeconds
        }
    }
}
