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
            val originalPriority = changeLog
                .filter(::isPriorityChange)
                .lastOrNull(::changedByStaff)
                ?.changedTo.getOrDefaultNull("-1")

            assertNotEquals(getId(priority), originalPriority).bind()
            updatePriority(originalPriority)
        }
    }

    private fun getId(priority: String?): String = when (priority) {
        "None" -> "-1"
        "Very Important" -> "11700"
        "Important" -> "11701"
        "Normal" -> "11702"
        "Low" -> "11703"
        else -> "-1"
    }

    private fun isPriorityChange(item: ChangeLogItem) =
        item.field == "Mojang Priority"

    private fun changedByStaff(item: ChangeLogItem) = !updateIsRecent(item) ||
        item.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.DAYS)
            .isAfter(Instant.now())
}
