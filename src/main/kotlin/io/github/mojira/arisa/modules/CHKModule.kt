package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

data class CHKModuleRequest(val issueId: String, val chkField: String?, val confirmationField: String?)

class CHKModule(val updateCHK: () -> Either<Throwable, Unit>) : Module<CHKModuleRequest> {
    override fun invoke(request: CHKModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        assertIsValid(request).bind()
        updateCHK().toFailedModuleEither().bind()
    }
}

private fun assertIsValid(request: CHKModuleRequest): Either<OperationNotNeededModuleResponse, Unit> = with(request) {
    when {
        confirmationField == null ||
            confirmationField == "Undefined" ||
            confirmationField.toLowerCase() == "unconfirmed" ||
            chkField != null -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
