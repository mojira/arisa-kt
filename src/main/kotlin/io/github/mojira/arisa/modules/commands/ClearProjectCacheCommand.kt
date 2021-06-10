package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.infrastructure.ProjectCache

class ClearProjectCacheCommand {
    operator fun invoke(): Int {
        ProjectCache.forceClear()
        return 1
    }
}
