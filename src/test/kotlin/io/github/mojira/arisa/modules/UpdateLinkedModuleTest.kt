package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.modules.UpdateLinkedModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

private val NOW = Instant.now()

class UpdateLinkedModuleTest : StringSpec({
    val A_SECOND_AGO = NOW.minusSeconds(1)
    val DUPLICATE_LINK = getChangeLogItem(
        changedTo = "This issue is duplicated by MC-4"
    )

    "should return OperationNotNeededModuleResponse when linked is empty and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, emptyList(), null) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked is 0 and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, emptyList(), 0.0) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked and number of duplicates is equal" {
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK), 1.0) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link" {
        val module = UpdateLinkedModule(1)
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK), 0.0) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link after the last Linked change" {
        val module = UpdateLinkedModule(1)
        val linkedChange = getChangeLogItem(
            created = NOW.minusSeconds(2),
            field = "Linked",
            changedFrom = "1.0"
        )
        val oldAddedLink = getChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            changedTo = "This issue is duplicated by MC-4"
        )
        val issue = getIssue(
            NOW.minus(3, ChronoUnit.HOURS),
            listOf(oldAddedLink, linkedChange, DUPLICATE_LINK),
            0.0
        ) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set linked when there are duplicates and linked is empty" {
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK), null) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should set linked when there are duplicates and linked is too low" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK), 0.0) { linked = it; Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 1.0
    }

    "should set linked when there are duplicates and linked is too high" {
        var linked = 1.0
        val module = UpdateLinkedModule(0)
        val removedLink = getChangeLogItem(
            created = NOW.plusSeconds(1),
            changedFrom = "This issue is duplicated by MC-4"
        )
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK, removedLink), 1.0) { linked = it; Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should only count duplicates" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val relatesLink = getChangeLogItem(
            changedTo = "This issue relates to MC-4"
        )
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK, relatesLink, DUPLICATE_LINK), null) {
            linked = it; Unit.right()
        }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 2.0
    }

    "should update if there is an old and a recent link" {
        val module = UpdateLinkedModule(1)
        val oldAddedLink = getChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            changedTo = "This issue is duplicated by MC-4"
        )
        val issue = getIssue(
            A_SECOND_AGO.minus(2, ChronoUnit.HOURS),
            listOf(oldAddedLink, DUPLICATE_LINK),
            null
        ) { Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should update if a link was removed" {
        var linked = 1.0
        val module = UpdateLinkedModule(1)
        val addedLink = getChangeLogItem(
            created = NOW.minus(4, ChronoUnit.HOURS),
            changedTo = "This issue is duplicated by MC-4"
        )
        val linkedChange = getChangeLogItem(
            created = A_SECOND_AGO.minus(3, ChronoUnit.HOURS),
            field = "Linked",
            changedFrom = "1.0"
        )
        val removedLink = getChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            changedFrom = "This issue is duplicated by MC-4"
        )
        val issue = getIssue(
            A_SECOND_AGO.minus(4, ChronoUnit.HOURS),
            listOf(addedLink, linkedChange, removedLink),
            1.0
        ) { linked = it; Unit.right() }

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should return FailedModuleResponse when setting linked fails" {
        val module = UpdateLinkedModule(0)
        val issue = getIssue(A_SECOND_AGO, listOf(DUPLICATE_LINK), null) { RuntimeException().left() }

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getUser() = User("user", "User")

private fun getChangeLogItem(
    created: Instant = NOW,
    field: String = "Link",
    changedFrom: String? = null,
    changedTo: String? = null,
    author: User = getUser(),
    getAuthorGroups: () -> List<String>? = { null }
) = ChangeLogItem(
    created,
    field,
    changedFrom,
    changedTo,
    author,
    getAuthorGroups
)
