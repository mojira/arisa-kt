package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.CommandDispatcher
import io.github.mojira.arisa.JiraConnectionService
import io.github.mojira.arisa.infrastructure.ProjectCache

interface CommandDispatcherFactory {
    /**
     * Creates a command dispatcher.
     *
     * @param prefix command prefix
     */
    fun createDispatcher(prefix: String): CommandDispatcher<CommandSource>

    companion object {
        fun createFactory(
            connectionService: JiraConnectionService,
            projectCache: ProjectCache
        ): CommandDispatcherFactory {
            return object : CommandDispatcherFactory {
                override fun createDispatcher(prefix: String): CommandDispatcher<CommandSource> {
                    return getCommandDispatcher(connectionService, projectCache, prefix)
                }
            }
        }
    }
}
