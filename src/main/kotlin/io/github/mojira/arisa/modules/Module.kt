package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

interface ModuleBase<TIssue> {
    operator fun invoke(issue: TIssue, lastRun: Instant): Either<ModuleError, ModuleResponse>

    // In case more details than just `lastRun` are needed, this function can be overridden
    operator fun invoke(
        issue: TIssue,
        timeframe: ExecutionTimeframe
    ): Either<ModuleError, ModuleResponse> =
        invoke(issue, timeframe.lastRunTime)
}

interface Module : ModuleBase<Issue>
interface CloudModule : ModuleBase<CloudIssue>

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
