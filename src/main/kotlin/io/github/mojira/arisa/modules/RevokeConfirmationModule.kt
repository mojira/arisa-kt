package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokeConfirmationModule : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val volunteerConfirmation = changeLog
                .filter(::isConfirmationChange)
                .lastOrNull(::changedByVolunteer)
                ?.changedToString.getOrDefault("Unconfirmed")

            assertNotEquals(confirmationStatus.getOrDefault("Unconfirmed"), volunteerConfirmation).bind()
            confirmationStatus = volunteerConfirmation
        }
    }

    private fun isConfirmationChange(item: ChangeLogItem) =
        item.field == "Confirmation Status"

    private fun changedByVolunteer(item: ChangeLogItem) = !updateIsRecent(item) ||
            item.author.groups.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.DAYS)
            .isAfter(Instant.now())
}
