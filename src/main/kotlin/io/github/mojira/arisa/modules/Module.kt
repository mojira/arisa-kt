package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue

interface Module {
    operator fun invoke(issue: Issue): Either<ModuleError, ModuleResponse>
}

sealed class Response(val issue: Issue)
class ModuleResponse(issue: Issue) : Response(issue)

sealed class ModuleError(issue: Issue) : Response(issue)
class OperationNotNeededModuleResponse(issue: Issue) : ModuleError(issue)
class FailedModuleResponse(issue: Issue, val exceptions: List<Throwable> = emptyList()) : ModuleError(issue)
