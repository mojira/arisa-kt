package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.ChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem

private const val CONFIRMATION_FIELD_NAME = "confField"

class RevokeConfirmationTest : StringSpec({

    "should return OperationNotNeededModuleResponse when Ticket is unconfirmed and confirmation was never changed" {
        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val request = RevokeConfirmationModuleRequest("Unconfirmed", emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by staff" {
        val module = RevokeConfirmationModule({ listOf("staff").right() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val changeLogEntry = mockChangelogEntry("", "Confirmed")
        val request = RevokeConfirmationModuleRequest("Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by helper" {
        val module = RevokeConfirmationModule({ listOf("helper").right() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val changeLogEntry = mockChangelogEntry("", "Confirmed")
        val request = RevokeConfirmationModuleRequest("Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by global-moderator" {
        val module = RevokeConfirmationModule({ listOf("global-moderators").right() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val changeLogEntry = mockChangelogEntry("", "Confirmed")
        val request = RevokeConfirmationModuleRequest("Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and groups cannot be gotten" {
        val module = RevokeConfirmationModule({ RuntimeException().left() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val changeLogEntry = mockChangelogEntry("", "Confirmed")
        val request = RevokeConfirmationModuleRequest("Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple volunteers changed the confirmation status" {
        val module = RevokeConfirmationModule({ listOf(it).right() }, { Unit.right() }, CONFIRMATION_FIELD_NAME)
        val volunteerChange = mockChangelogEntry("staff", "Confirmed")
        val userChange = mockChangelogEntry("helper", "Unconfirmed")
        val request = RevokeConfirmationModuleRequest("Unconfirmed", listOf(volunteerChange, userChange))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Unconfirmed when ticket was created Confirmed" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { changedConfirmation = it; Unit.right() }, CONFIRMATION_FIELD_NAME)
        val request = RevokeConfirmationModuleRequest("Confirmed", emptyList())

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when ticket was Confirmed by a non-volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { changedConfirmation = it; Unit.right() }, CONFIRMATION_FIELD_NAME)
        val changeLogEntry = mockChangelogEntry("", "Confirmed")
        val request = RevokeConfirmationModuleRequest("Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set back to status set by volunteer, when regular user changes confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ listOf(it).right() }, { changedConfirmation = it; Unit.right() }, CONFIRMATION_FIELD_NAME)
        val volunteerChange = mockChangelogEntry("staff", "Confirmed")
        val userChange = mockChangelogEntry("users", "Unconfirmed")
        val request = RevokeConfirmationModuleRequest("Unconfirmed", listOf(volunteerChange, userChange))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should return FailedModuleResponse when changing confirmation status fails" {
        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { RuntimeException().left() }, CONFIRMATION_FIELD_NAME)
        val request = RevokeConfirmationModuleRequest("Confirmed", emptyList())

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun mockChangelogEntry(authorName: String, confirmationStatus: String): ChangeLogEntry {
    val changeLogEntry = mockk<ChangeLogEntry>()
    every { changeLogEntry.author.name } returns authorName
    every { changeLogEntry.items } returns listOf(mockChangelogItem(confirmationStatus))

    return changeLogEntry
}

private fun mockChangelogItem(confirmationStatus: String): ChangeLogItem {
    val changeLogItem = mockk<ChangeLogItem>()
    every { changeLogItem.field } returns CONFIRMATION_FIELD_NAME
    every { changeLogItem.toString } returns confirmationStatus

    return changeLogItem
}
