package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import net.rcarz.jiraclient.Issue

data class CHKModuleRequest(
    val issue: Issue,
    val chkField: String?,
    val confirmationField: String?
)

class CHKModule(val updateCHK: (Issue) -> Either<Throwable, Unit>) : Module<CHKModuleRequest> {
    override fun invoke(request: CHKModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertIsValid(request).bind()
            updateCHK(issue).toFailedModuleEither().bind()
        }
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
