package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse

class ModuleExecutor(
    private val config: Config,
    private val moduleConfig: Arisa.Modules.ModuleConfigSpec
) {
    fun executeModule(
        allIssues: List<Issue>,
        addFailedTicket: (String) -> Unit,
        execute: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        getIssuesForModule(allIssues)
            .map { it.key to execute(it) }
            .forEach { (issue, response) ->
                val moduleName = response.first
                response.second.fold(
                    {
                        @Suppress("detekt:OptionalWhenBraces")
                        when (it) {
                            is OperationNotNeededModuleResponse -> {
                                if (config[Arisa.Debug.logOperationNotNeeded]) {
                                    log.debug("[RESPONSE] [$issue] [$moduleName] Operation not needed")
                                }
                            }

                            is FailedModuleResponse -> {
                                addFailedTicket(issue)

                                for (exception in it.exceptions) {
                                    log.error("[RESPONSE] [$issue] [$moduleName] Failed", exception)
                                }
                            }
                        }
                    },
                    {
                        log.info("[RESPONSE] [$issue] [$moduleName] Successful")
                    }
                )

                IssueUpdateContextCache.updateTriggeredBy(issue)
            }

        IssueUpdateContextCache.applyChanges(addFailedTicket)
    }

    private fun getIssuesForModule(allIssues: List<Issue>): List<Issue> {
        val projects = config[moduleConfig.projects] ?: config[Arisa.Issues.projects]

        val resolutions = (config[moduleConfig.resolutions] ?: config[Arisa.Issues.resolutions])
            .map(String::lowercase)

        val excludedStatuses = config[moduleConfig.excludedStatuses].map(String::lowercase)

        return allIssues
            .filter { it.project.key in projects }
            .filter { it.status.lowercase() !in excludedStatuses }
            .filter { (it.resolution?.lowercase() ?: "unresolved") in resolutions }
    }
}
