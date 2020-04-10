package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx

class RevokeConfirmationModule : Module<RevokeConfirmationModule.Request> {
    data class ChangeLogItem(
        val field: String,
        val newValue: String,
        val authorGroups: List<String>?
    )

    data class Request(
        val confirmationStatus: String?,
        val changeLog: List<ChangeLogItem>,
        val setConfirmationStatus: (String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val volunteerConfirmation = changeLog
                .filter(::isConfirmationChange)
                .filter(::changedByVolunteer)
                .lastOrNull()
                ?.newValue ?: "Unconfirmed"

            assertNotEquals(confirmationStatus, volunteerConfirmation).bind()
            setConfirmationStatus(volunteerConfirmation).toFailedModuleEither().bind()
        }
    }

    private fun isConfirmationChange(item: ChangeLogItem) =
        item.field == "Confirmation Status"

    private fun changedByVolunteer(item: ChangeLogItem) =
        item.authorGroups?.any { it == "helper" || it == "global-moderator" || it == "staff" } ?: true
}
