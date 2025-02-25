package io.github.mojira.arisa.registry

import arrow.core.Either
import arrow.core.left
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.ModuleExecutor
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleBase
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse

// All defined module registries
val getModuleRegistries = { config: Config ->
    listOf(
        InstantModuleRegistry(config)
    )
}

abstract class ModuleRegistry<TIssue>(protected val config: Config) {
    data class Entry<TIssue>(
        val name: String,
        val config: ModuleConfigSpec,
        val execute: (issue: TIssue, timeframe: ExecutionTimeframe) -> Pair<String, Either<ModuleError, ModuleResponse>>,
        val executor: ModuleExecutor
    )

    private val modules = mutableListOf<Entry<TIssue>>()

    protected abstract fun getJql(timeframe: ExecutionTimeframe): String

    fun getAllModules(): List<Entry<TIssue>> = modules

    fun getEnabledModules(): List<Entry<TIssue>> = modules.filter(::isModuleEnabled)

    private fun isModuleEnabled(module: Entry<TIssue>) =
        // If arisa.debug.enabledModules is defined, return whether that module is in that list.
        // If it's not defined, return whether this module is enabled in the module config.
        config[Arisa.Debug.enabledModules]?.contains(module.name) ?: config[module.config.enabled]

    protected fun register(
        moduleConfig: ModuleConfigSpec,
        module: ModuleBase<TIssue>
    ) {
        val moduleName = moduleConfig::class.simpleName!!

        modules.add(
            Entry(
                moduleName,
                moduleConfig,
                getModuleResult(moduleName, module),
                ModuleExecutor(config, moduleConfig)
            )
        )
    }

    private fun getModuleResult(moduleName: String, module: ModuleBase<TIssue>) =
        { issue: TIssue, timeframe: ExecutionTimeframe ->
            moduleName to tryExecuteModule { module(issue, timeframe) }
        }

    private fun getJqlWithDebug(timeframe: ExecutionTimeframe): String {
        val registryJql = getJql(timeframe)

        val debugWhitelist = config[Arisa.Debug.ticketWhitelist]

        return if (debugWhitelist == null) {
            registryJql
        } else {
            "key IN (${debugWhitelist.joinToString()}) AND ($registryJql)"
        }
    }

    fun getFullJql(timeframe: ExecutionTimeframe, failedTickets: Collection<String>): String {
        val failedTicketsJql = if (failedTickets.isEmpty()) "" else "key in (${failedTickets.joinToString()}) OR "
        val registryJql = "(${getJqlWithDebug(timeframe)})"

        return "$failedTicketsJql$registryJql ORDER BY updated ASC"
    }

    @Suppress("TooGenericExceptionCaught")
    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }
}
