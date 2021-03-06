package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

abstract class Module {
    operator fun invoke(issue: Issue, lastRun: Instant): Pair<Issue, Either<ModuleError, ModuleResponse>> =
        issue to execute(issue, lastRun)

    abstract fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
