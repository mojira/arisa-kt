package io.github.mojira.arisa.modules

import arrow.core.Either

interface Module<REQUEST> {
    operator fun invoke(request: REQUEST): Either<ModuleError, ModuleResponse>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()
