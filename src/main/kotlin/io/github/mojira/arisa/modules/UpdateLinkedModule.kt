package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

val DUPLICATE_REGEX = """This issue is duplicated by [A-Z]+-[0-9]+""".toRegex()

class UpdateLinkedModule(
    private val updateInterval: Long
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val lastLinkedChange = changeLog
                .lastOrNull(::isLinkedChange)
                ?.created
                ?: created

            val duplicates = changeLog.filter(::isDuplicateLinkChange)
            val duplicatesAdded = duplicates
                .filter(::isDuplicateLinkAddedChange)
                .size
            val duplicatesRemoved = duplicates
                .filter(::isDuplicateLinkRemovedChange)
                .size
            val duplicateAmount = (duplicatesAdded - duplicatesRemoved).toDouble()

            assertNotEquals(duplicateAmount, linked ?: 0.0).bind()

            val firstAddedLinkSinceLastUpdate = duplicates
                .firstOrNull(::createdAfter.partially2(lastLinkedChange))
                ?.created

            assertNotNull(firstAddedLinkSinceLastUpdate).bind()
            assertLinkNotAddedRecently(firstAddedLinkSinceLastUpdate!!).bind()

            updateLinked(duplicateAmount)
        }
    }

    private fun isLinkedChange(change: ChangeLogItem) =
        change.field == "Linked"

    private fun isDuplicateLinkAddedChange(change: ChangeLogItem) =
        change.field == "Link" &&
                change.changedTo?.matches(DUPLICATE_REGEX) ?: false

    private fun isDuplicateLinkRemovedChange(change: ChangeLogItem) =
        change.field == "Link" &&
                change.changedFrom?.matches(DUPLICATE_REGEX) ?: false

    private fun isDuplicateLinkChange(change: ChangeLogItem) =
        change.field == "Link" && (
                isDuplicateLinkAddedChange(change) || isDuplicateLinkRemovedChange(change)
                )

    private fun createdAfter(change: ChangeLogItem, lastUpdate: Instant) =
        change.created.isAfter(lastUpdate)

    private fun assertLinkNotAddedRecently(lastUpdate: Instant) =
        when {
            lastUpdate
                .isBefore(
                    Instant.now().minus(updateInterval, ChronoUnit.HOURS)
                ) -> Unit.right()
            else -> OperationNotNeededModuleResponse.left()
        }
}
