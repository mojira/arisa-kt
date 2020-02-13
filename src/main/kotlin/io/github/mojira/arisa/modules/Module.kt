package io.github.mojira.arisa.modules

import arrow.core.Either
import com.uchuhimo.konf.Config
import net.rcarz.jiraclient.JiraClient

interface Module<REQUEST> {
    operator fun invoke(request: REQUEST): Either<ModuleError, ModuleResponse>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse: ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable>): ModuleError()