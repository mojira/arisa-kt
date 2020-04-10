package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

class CHKModule : Module<CHKModule.Request> {
    data class Request(
        val updateCHK: () -> Either<Throwable, Unit>,
        val chkField: String?,
        val confirmationField: String?
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertConfirmed(confirmationField).bind()
            assertNoChk(chkField).bind()
            updateCHK().toFailedModuleEither().bind()
        }
    }

    private fun assertConfirmed(confirmationField: String?) =
        when(confirmationField?.toLowerCase()) {
            null -> OperationNotNeededModuleResponse.left()
            "undefined" -> OperationNotNeededModuleResponse.left()
            "unconfirmed" -> OperationNotNeededModuleResponse.left()
            else -> Unit.right()
        }

    private fun assertNoChk(chkField: String?) =
        when (chkField) {
            null -> OperationNotNeededModuleResponse.left()
            else -> Unit.right()
        }
}
