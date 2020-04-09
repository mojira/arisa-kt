package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.ChangeLogEntry
import net.rcarz.jiraclient.Issue

data class RevokeConfirmationModuleRequest(
    val issue: Issue,
    val confirmationStatus: String?,
    val changeLog: List<ChangeLogEntry>
)

class RevokeConfirmationModule(
    private val getGroups: (String) -> Either<Throwable, List<String>>,
    private val setConfirmationStatus: (Issue, String) -> Either<Throwable, Unit>
) : Module<RevokeConfirmationModuleRequest> {
    override fun invoke(request: RevokeConfirmationModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val confirmationChanges = changeLog.filter(::isConfirmationChange)
            val volunteerChanges = confirmationChanges.filter(::changedByVolunteer)
            val lastVolunteerChange = volunteerChanges.lastOrNull()
            val volunteerConfirmation = lastVolunteerChange?.let { getConfirmation(it) } ?: "Unconfirmed"

            assertNotEquals(confirmationStatus, volunteerConfirmation).bind()

            setConfirmationStatus(issue, volunteerConfirmation).toFailedModuleEither().bind()
        }
    }

    private fun isConfirmationChange(entry: ChangeLogEntry) =
        entry.items.any { it.field == "Confirmation Status" }

    private fun getConfirmation(entry: ChangeLogEntry) =
        entry.items.lastOrNull { it.field == "Confirmation Status" }?.toString

    private fun changedByVolunteer(entry: ChangeLogEntry): Boolean {
        val groups = getGroups(entry.author.name)

        return if (groups.isLeft())
        // when in doubt, assume change was done by a volunteer
            true
        else
            (groups as Either.Right).b.any { it == "helper" || it == "global-moderators" || it == "staff" }
    }
}
