package io.github.mojira.arisa.application

interface Module<REQUEST> {
    operator fun invoke(request: REQUEST): ModuleResponse
}

sealed class ModuleResponse
object SucessfulModuleResponse: ModuleResponse()
object OperationNotNeededModuleResponse: ModuleResponse()
data class FailedModuleResponse(val exceptions: List<Exception>): ModuleResponse()