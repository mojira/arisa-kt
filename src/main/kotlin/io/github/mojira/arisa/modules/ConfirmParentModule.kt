package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class ConfirmParentModule(
    private val confirmationStatusWhitelist: List<String>,
    private val targetConfirmationStatus: String,
    private val linkedThreshold: Double
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertLinkedMoreThanThreshold(linked).bind()
            assertConfirmationStatusWhitelisted(confirmationStatus, confirmationStatusWhitelist).bind()
            updateConfirmationStatus(targetConfirmationStatus).toFailedModuleEither().bind()
        }
    }

    private fun assertLinkedMoreThanThreshold(linked: Double?) = if ((linked ?: 0.0) >= linkedThreshold) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }

    private fun assertConfirmationStatusWhitelisted(status: String?, whitelist: List<String>) =
        if ((status.getOrDefault("Unconfirmed")) in whitelist) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }

    private fun String?.getOrDefault(default: String) =
        if (isNullOrBlank())
            default
        else
            this!!
}
