package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokeConfirmationModule : Module<RevokeConfirmationModule.Request> {
    data class ChangeLogItem(
        val field: String,
        val newValue: String?,
        val created: Instant,
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
                ?.newValue.getOrDefault("Unconfirmed")

            assertNotEquals(confirmationStatus, volunteerConfirmation).bind()
            setConfirmationStatus(volunteerConfirmation).toFailedModuleEither().bind()
        }
    }

    private fun isConfirmationChange(item: ChangeLogItem) =
        item.field == "Confirmation Status"

    private fun changedByVolunteer(item: ChangeLogItem) =
        !updateIsRecent(item) || item.authorGroups?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) = item
        .created
        .plus(1, ChronoUnit.DAYS)
        .isAfter(Instant.now())

    private fun String?.getOrDefault(default: String) =
        if (isNullOrBlank())
            default
        else
            this!!
}
