package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokeConfirmationModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when Ticket is unconfirmed and confirmation was never changed" {
        val module = RevokeConfirmationModule()
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed"
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by staff" {
        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by helper" {
        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("helper") }
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by global-moderator" {
        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("global-moderators") }
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket was confirmed more than a day ago by a user who is no longer staff" {
        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem(Instant.now().minus(2, ChronoUnit.DAYS))
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and groups are unknown" {
        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { null }
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple volunteers changed the confirmation status" {
        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherVolunteerChange = mockChangeLogItem(value = "Unconfirmed") { listOf("helper") }
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            changeLog = listOf(volunteerChange, otherVolunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is Unconfirmed and Confirmation Status was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherVolunteerChange = mockChangeLogItem(value = "") { listOf("helper") }
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            changeLog = listOf(volunteerChange, otherVolunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when confirmation status is null and was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherVolunteerChange = mockChangeLogItem(value = "") { listOf("helper") }
        val issue = mockIssue(
            changeLog = listOf(volunteerChange, otherVolunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when confirmation status is empty and was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherVolunteerChange = mockChangeLogItem(value = "") { listOf("helper") }
        val issue = mockIssue(
            confirmationStatus = "",
            changeLog = listOf(volunteerChange, otherVolunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Unconfirmed when ticket was created Confirmed" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when there is a changelog entry unrelated to confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val changeLogItem =
            io.github.mojira.arisa.modules.mockChangeLogItem(field = "Totally Not Confirmation Status") { listOf("staff") }
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem),
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when ticket was Confirmed by a non-volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem()
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            changeLog = listOf(changeLogItem),
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when ticket when confirmation status is removed by a non-volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem(value = "") { listOf("users") }
        val issue = mockIssue(
            confirmationStatus = "",
            changeLog = listOf(changeLogItem),
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set back to status set by volunteer, when regular user changes confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val userChange = mockChangeLogItem(value = "Unconfirmed") { listOf("users") }
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            changeLog = listOf(volunteerChange, userChange),
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should set back to previous status when regular user changes confirmation status set by someone who no longer is a volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem(Instant.now().minus(2, ChronoUnit.DAYS))
        val userChange = mockChangeLogItem(value = "Unconfirmed") { listOf("users") }
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            changeLog = listOf(volunteerChange, userChange),
            updateConfirmationStatus = { changedConfirmation = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }
})

private fun mockChangeLogItem(
    created: Instant = RIGHT_NOW,
    field: String = "Confirmation Status",
    value: String = "Confirmed",
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = mockChangeLogItem(
    created = created,
    field = field,
    changedToString = value,
    getAuthorGroups = getAuthorGroups
)
