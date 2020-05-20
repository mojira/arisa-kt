package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.temporal.ChronoUnit

private val A_SECOND_AGO = NOW.minusSeconds(1)
private val DUPLICATE_LINK = mockChangeLogItem(
    field = "Link",
    changedTo = "This issue is duplicated by MC-4"
)

class UpdateLinkedModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when linked is empty and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked is 0 and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            linked = 0.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked and number of duplicates is equal" {
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK),
            linked = 1.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link" {
        val module = UpdateLinkedModule(1)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK),
            linked = 0.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link after the last Linked change" {
        val module = UpdateLinkedModule(1)
        val linkedChange = mockChangeLogItem(
            created = NOW.minusSeconds(2),
            field = "Linked",
            changedFrom = "1.0"
        )
        val oldAddedLink = mockChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            field = "Link",
            changedTo = "This issue is duplicated by MC-4"
        )
        val issue = mockIssue(
            created = NOW.minus(3, ChronoUnit.HOURS),
            changeLog = listOf(oldAddedLink, linkedChange, DUPLICATE_LINK),
            linked = 0.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set linked when there are duplicates and linked is empty" {
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should set linked when there are duplicates and linked is too low" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK),
            linked = 0.0,
            updateLinked = { linked = it; Unit.right() }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 1.0
    }

    "should set linked when there are duplicates and linked is too high" {
        var linked = 1.0
        val module = UpdateLinkedModule(0)
        val removedLink = mockChangeLogItem(
            created = NOW.plusSeconds(1),
            field = "Link",
            changedFrom = "This issue is duplicated by MC-4"
        )
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK, removedLink),
            linked = 1.0,
            updateLinked = { linked = it; Unit.right() }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should only count duplicates" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val relatesLink = mockChangeLogItem(
            field = "Link",
            changedTo = "This issue relates to MC-4"
        )
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK, relatesLink, DUPLICATE_LINK),
            updateLinked = {
                linked = it; Unit.right()
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 2.0
    }

    "should update if there is an old and a recent link" {
        val module = UpdateLinkedModule(1)
        val oldAddedLink = mockChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            field = "Link",
            changedTo = "This issue is duplicated by MC-4"
        )
        val issue = mockIssue(
            created = A_SECOND_AGO.minus(2, ChronoUnit.HOURS),
            changeLog = listOf(oldAddedLink, DUPLICATE_LINK)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should update if a link was removed" {
        var linked = 1.0
        val module = UpdateLinkedModule(1)
        val addedLink = mockChangeLogItem(
            created = NOW.minus(4, ChronoUnit.HOURS),
            field = "Link",
            changedTo = "This issue is duplicated by MC-4"
        )
        val linkedChange = mockChangeLogItem(
            created = A_SECOND_AGO.minus(3, ChronoUnit.HOURS),
            field = "Linked",
            changedFrom = "1.0"
        )
        val removedLink = mockChangeLogItem(
            created = NOW.minus(2, ChronoUnit.HOURS),
            field = "Link",
            changedFrom = "This issue is duplicated by MC-4"
        )
        val issue = mockIssue(
            created = A_SECOND_AGO.minus(4, ChronoUnit.HOURS),
            changeLog = listOf(addedLink, linkedChange, removedLink),
            linked = 1.0,
            updateLinked = { linked = it; Unit.right() }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should return FailedModuleResponse when setting linked fails" {
        val module = UpdateLinkedModule(0)
        val issue = mockIssue(
            created = A_SECOND_AGO,
            changeLog = listOf(DUPLICATE_LINK),
            updateLinked = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
