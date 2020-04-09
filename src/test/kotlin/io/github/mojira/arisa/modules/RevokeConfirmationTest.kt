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
import net.rcarz.jiraclient.Issue

class RevokeConfirmationTest : StringSpec({
    val ISSUE = mockk<Issue>()

    "should return OperationNotNeededModuleResponse when Ticket is unconfirmed and confirmation was never changed" {
        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { _, _ -> Unit.right() })
        val request = RevokeConfirmationModuleRequest(ISSUE, "Unconfirmed", emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by staff" {
        val module = RevokeConfirmationModule({ listOf("staff").right() }, { _, _ -> Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by helper" {
        val module = RevokeConfirmationModule({ listOf("helper").right() }, { _, _ -> Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by global-moderator" {
        val module = RevokeConfirmationModule({ listOf("global-moderators").right() }, { _, _ -> Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and groups cannot be gotten" {
        val module = RevokeConfirmationModule({ RuntimeException().left() }, { _, _ -> Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple volunteers changed the confirmation status" {
        val module = RevokeConfirmationModule({ listOf(it).right() }, { _, _ -> Unit.right() })
        val volunteerChange = mockChangelogEntry("staff", "Confirmation Status", "Confirmed")
        val userChange = mockChangelogEntry("helper", "Confirmation Status", "Unconfirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Unconfirmed", listOf(volunteerChange, userChange))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Unconfirmed when ticket was created Confirmed" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { _, it -> changedConfirmation = it; Unit.right() })
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", emptyList())

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when there is a changelog entry unrelated to confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { _, it -> changedConfirmation = it; Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Totally Not Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when ticket was Confirmed by a non-volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { _, it -> changedConfirmation = it; Unit.right() })
        val changeLogEntry = mockChangelogEntry("", "Confirmation Status", "Confirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", listOf(changeLogEntry))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set back to status set by volunteer, when regular user changes confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule({ listOf(it).right() }, { _, it -> changedConfirmation = it; Unit.right() })
        val volunteerChange = mockChangelogEntry("staff", "Confirmation Status", "Confirmed")
        val userChange = mockChangelogEntry("users", "Confirmation Status", "Unconfirmed")
        val request = RevokeConfirmationModuleRequest(ISSUE, "Unconfirmed", listOf(volunteerChange, userChange))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should return FailedModuleResponse when changing confirmation status fails" {
        val module = RevokeConfirmationModule({ emptyList<String>().right() }, { _, _ -> RuntimeException().left() })
        val request = RevokeConfirmationModuleRequest(ISSUE, "Confirmed", emptyList())

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun mockChangelogEntry(authorName: String, fieldName: String, fieldValue: String): ChangeLogEntry {
    val changeLogEntry = mockk<ChangeLogEntry>()
    every { changeLogEntry.author.name } returns authorName
    every { changeLogEntry.items } returns listOf(mockChangelogItem(fieldName, fieldValue))

    return changeLogEntry
}

private fun mockChangelogItem(name: String, value: String): ChangeLogItem {
    val changeLogItem = mockk<ChangeLogItem>()
    every { changeLogItem.field } returns name
    every { changeLogItem.toString } returns value

    return changeLogItem
}
