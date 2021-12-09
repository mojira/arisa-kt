package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

interface Module {
    operator fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse>

    // In case more details than just `lastRun` are needed, this function can be overridden
    operator fun invoke(
        issue: Issue,
        timeframe: ExecutionTimeframe
    ): Either<ModuleError, ModuleResponse> =
        invoke(issue, timeframe.lastRunTime)
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
