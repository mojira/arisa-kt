package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class CHKModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertConfirmed(confirmationStatus).bind()
            assertNoChk(chkField).bind()
            updateCHK().toFailedModuleEither().bind()
        }
    }

    private fun assertConfirmed(confirmationField: String?) =
        when (confirmationField?.toLowerCase()) {
            null -> OperationNotNeededModuleResponse.left()
            "undefined" -> OperationNotNeededModuleResponse.left()
            "unconfirmed" -> OperationNotNeededModuleResponse.left()
            else -> Unit.right()
        }

    private fun assertNoChk(chkField: String?) =
        when (chkField) {
            null -> Unit.right()
            else -> OperationNotNeededModuleResponse.left()
        }
}
