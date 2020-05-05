package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.modules.RevokeConfirmationModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokeConfirmationTest : StringSpec({
    "should return OperationNotNeededModuleResponse when Ticket is unconfirmed and confirmation was never changed" {
        val module = RevokeConfirmationModule()
        val request = Request("Unconfirmed", emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by staff" {
        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val request = Request("Confirmed", listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by helper" {
        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("helper") }
        val request = Request("Confirmed", listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and was changed by global-moderator" {
        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("global-moderators") }
        val request = Request("Confirmed", listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket was confirmed more than a day ago by a user who is no longer staff" {
        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(
            Instant.now().minus(2, ChronoUnit.DAYS),
            "Confirmation Status",
            "Confirmed"
        ) { emptyList() }
        val request = Request("Confirmed", listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket is confirmed and groups are unknown" {
        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { null }
        val request = Request("Confirmed", listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple volunteers changed the confirmation status" {
        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val otherVolunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Unconfirmed") { listOf("helper") }
        val request = Request("Unconfirmed", listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is Unconfirmed and Confirmation Status was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val otherVolunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "") { listOf("helper") }
        val request = Request("Unconfirmed", listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when confirmation status is null and was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val otherVolunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "") { listOf("helper") }
        val request = Request(null, listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when confirmation status is empty and was unset" {
        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val otherVolunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "") { listOf("helper") }
        val request = Request("", listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Unconfirmed when ticket was created Confirmed" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val request = Request("Confirmed", emptyList()) { changedConfirmation = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when there is a changelog entry unrelated to confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Totally Not Confirmation Status", "Confirmed") { listOf("staff") }
        val request = Request("Confirmed", listOf(changeLogItem)) { changedConfirmation = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to Unconfirmed when ticket was Confirmed by a non-volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val changeLogItem = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { emptyList() }
        val request = Request("Confirmed", listOf(changeLogItem)) { changedConfirmation = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set back to status set by volunteer, when regular user changes confirmation status" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Confirmed") { listOf("staff") }
        val userChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Unconfirmed") { listOf("users") }
        val request = Request("Unconfirmed", listOf(volunteerChange, userChange)) { changedConfirmation = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should set back to previous status when regular user changes confirmation status set by someone who no longer is a volunteer" {
        var changedConfirmation = ""

        val module = RevokeConfirmationModule()
        val volunteerChange = getChangeLogItem(
            Instant.now().minus(2, ChronoUnit.DAYS),
            "Confirmation Status",
            "Confirmed"
        ) { emptyList() }
        val userChange = getChangeLogItem(Instant.now(), "Confirmation Status", "Unconfirmed") { listOf("users") }
        val request = Request("Unconfirmed", listOf(volunteerChange, userChange)) { changedConfirmation = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should return FailedModuleResponse when changing confirmation status fails" {
        val module = RevokeConfirmationModule()
        val request = Request("Confirmed", emptyList()) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

fun getChangeLogItem(
    created: Instant,
    field: String,
    value: String,
    getAuthorGroups: () -> List<String>?
) =
    ChangeLogItem(created, field, value, null, getAuthorGroups)
