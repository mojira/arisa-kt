package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokePriorityModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val prioritySet = changeLog
                .filter(::isPriorityChange)
                .filter(::changedByPermitted)
                .lastOrNull()
                ?.changedToString.getOrDefault("None")

            assertNotEquals(priority.getOrDefault("None"), prioritySet).bind()
            updatePriority(prioritySet)
        }
    }

    private fun isPriorityChange(item: ChangeLogItem) =
        item.field == "Mojang Priority"

    private fun changedByPermitted(item: ChangeLogItem) = !updateIsRecent(item) ||
            item.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.DAYS)
            .isAfter(Instant.now())

    private fun String?.getOrDefault(default: String) =
        if (isNullOrBlank())
            default
        else
            this!!
}
