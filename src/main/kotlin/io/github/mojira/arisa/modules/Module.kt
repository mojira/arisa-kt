package io.github.mojira.arisa.modules

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue

interface Module {
    operator fun invoke(issue: Issue): Pair<Issue, Either<ModuleError, ModuleResponse>>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
