package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

class ConfirmParentModule(
    private val confirmationStatusWhitelist: List<String>,
    private val targetConfirmationStatus: String,
    private val linkedThreshold: Double
) : Module<ConfirmParentModule.Request> {
    data class Request(
        val confirmationStatus: String?,
        val linked: Double?,
        val setConfirmationStatus: (String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: ConfirmParentModule.Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertLinkedMoreThanZero(linked).bind()
            assertConfirmationStatusWhitelisted(confirmationStatus, confirmationStatusWhitelist).bind()
            setConfirmationStatus(targetConfirmationStatus).toFailedModuleEither().bind()
        }
    }

    private fun assertLinkedMoreThanZero(linked: Double?) = if ((linked ?: 0.0) >= linkedThreshold) {
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
