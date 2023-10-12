package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokePriorityModule : Module {
    val log: Logger = LoggerFactory.getLogger("RevokePriorityModule")

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            // Check if there are recent changes to priority in the changelog
            val recentPriorityChanges = changeLog
                .filter { item -> item.created >= lastRun }
                .filter(::isPriorityChange)

            // If there aren't any recent changes to priority, there's nothing to revoke
            assertNotEmpty(recentPriorityChanges).bind()

            // Try to recover original priority by getting entries from the changelog
            val originalPriorityItem = changeLog
                .filter(::isPriorityChange)
                .lastOrNull(::changedByStaff)

            val originalPriorityId = originalPriorityItem?.changedTo.getOrDefaultNull("-1")
            val originalPriorityName = originalPriorityItem?.changedToString.getOrDefaultNull("None")

            // Check whether the original priority differs from the current priority
            assertNotEquals(getId(priority), originalPriorityId).bind()

            // Ensure that the priority from the changelog is a currently valid priority value
            // (e.g. 11602 was the original value for 'Normal', which is no longer valid)
            assertValidPriority(originalPriorityId).mapLeft { result ->
                log.error(
                    "[${issue.key}] Cannot revoke change to priority: " +
                        "Unknown mojang priority value '$originalPriorityName' [$originalPriorityId]"
                )
                result
            }.bind()

            updatePriority(originalPriorityId)
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

    private fun assertValidPriority(priorityId: String) =
        if (isValidPriority(priorityId)) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }

    private fun isValidPriority(priority: String): Boolean =
        setOf("-1", "11700", "11701", "11702", "11703").contains(priority)

    private fun isPriorityChange(item: ChangeLogItem) =
        item.field == "Mojang Priority"

    private fun changedByStaff(item: ChangeLogItem) = !updateIsRecent(item) ||
        item.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.YEARS)
            .isAfter(Instant.now())
}
