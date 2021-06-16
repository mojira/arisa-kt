package io.github.mojira.arisa

import com.uchuhimo.konf.Config

class ExecutionService(
    config: Config,
    private val connectionService: JiraConnectionService
) {
    private val executor = Executor(config)
    private val lastRun = LastRun(config)

    /**
     * @return amount of seconds to sleep after this execution cycle
     */
    fun runExecutionCycle(): Long {
        HelperMessageUpdateService.checkForUpdate()

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
