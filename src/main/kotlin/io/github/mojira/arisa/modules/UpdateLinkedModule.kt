package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import java.time.Instant
import java.time.temporal.ChronoUnit

val DUPLICATE_REGEX = """This issue is duplicated by [A-Z]+-[0-9]+""".toRegex()

class UpdateLinkedModule(
    private val updateInterval: Long
) : Module<UpdateLinkedModule.Request> {
    data class ChangeLogItem(
        val field: String,
        val created: Instant,
        val oldValue: String?,
        val newValue: String?
    )

    data class Request(
        val created: Instant,
        val changeLogItems: List<ChangeLogItem>,
        val linkedField: Double?,
        val setLinked: (Double) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val lastLinkedChange = changeLogItems
                .lastOrNull(::isLinkedChange)
                ?.created
                ?: created

            val duplicates = changeLogItems.filter(::isDuplicateLinkChange)
            val duplicatesAdded = duplicates
                .filter(::isDuplicateLinkAddedChange)
                .size
            val duplicatesRemoved = duplicates
                .filter(::isDuplicateLinkRemovedChange)
                .size
            val duplicateAmount = (duplicatesAdded - duplicatesRemoved).toDouble()

            assertNotEquals(duplicateAmount, linkedField ?: 0.0).bind()

            val firstAddedLinkSinceLastUpdate = duplicates
                .firstOrNull(::createdAfter.partially2(lastLinkedChange))
                ?.created

            assertNotNull(firstAddedLinkSinceLastUpdate).bind()
            assertLinkNotAddedRecently(firstAddedLinkSinceLastUpdate!!).bind()

            setLinked(duplicateAmount).toFailedModuleEither().bind()
        }
    }

    private fun isLinkedChange(change: ChangeLogItem) =
        change.field == "Linked"

    private fun isDuplicateLinkAddedChange(change: ChangeLogItem) =
        change.field == "Link" &&
        change.newValue?.matches(DUPLICATE_REGEX) ?: false

    private fun isDuplicateLinkRemovedChange(change: ChangeLogItem) =
        change.field == "Link" &&
        change.oldValue?.matches(DUPLICATE_REGEX) ?: false

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
