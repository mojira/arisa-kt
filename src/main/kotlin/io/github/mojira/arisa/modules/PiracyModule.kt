package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import net.rcarz.jiraclient.Issue

data class PiracyModuleRequest(val issue: Issue, val environment: String?, val description: String?)

class PiracyModule : Module<PiracyModuleRequest> {

    override fun invoke(request: PiracyModuleRequest): Either<ModuleError, ModuleResponse> {
        if (request.description.isNullOrEmpty() && request.environment.isNullOrEmpty()) {
            return OperationNotNeededModuleResponse.left()
        }
        TODO()
    }
}
