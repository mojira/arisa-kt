package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

interface Module {
    operator fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
