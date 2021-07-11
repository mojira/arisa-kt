package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.infrastructure.ProjectCache

class ClearProjectCacheCommand(private val projectCache: ProjectCache) {
    operator fun invoke(): Int {
        projectCache.forceClear()
        return 1
    }
}
