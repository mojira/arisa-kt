package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.RevokeFieldChangesModule.ChangeLogItem
import io.github.mojira.arisa.modules.RevokeFieldChangesModule.Field
import io.github.mojira.arisa.modules.RevokeFieldChangesModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class RevokeFieldChangesModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no fields" {
        val module = RevokeFieldChangesModule()
        val request = Request(0, emptyList(), emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field is default value and was never changed" {
        val module = RevokeFieldChangesModule()
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            emptyList(),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field was changed by member of whitelisted group" {
        val module = RevokeFieldChangesModule()
        val changeLogItem = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field was changed before the last run" {
        val module = RevokeFieldChangesModule()
        val changeLogItem = ChangeLogItem("Confirmation Status", "Confirmed", 0) { emptyList() }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(1, listOf(field), listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field was changed and groups are unknown" {
        val module = RevokeFieldChangesModule()
        val changeLogItem = ChangeLogItem("Confirmation Status", "Confirmed", 1) { null }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple volunteers changed the field" {
        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val otherVolunteerChange = ChangeLogItem("Confirmation Status", "Unconfirmed", 2) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field is default value and was unset" {
        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val otherVolunteerChange = ChangeLogItem("Confirmation Status", "", 2) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field is null and was unset" {
        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val otherVolunteerChange = ChangeLogItem("Confirmation Status", "", 2) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            null,
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field is empty and was unset" {
        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val otherVolunteerChange = ChangeLogItem("Confirmation Status", "", 2) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when field is default and was set to null" {
        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val otherVolunteerChange = ChangeLogItem("Confirmation Status", "null", 2) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, otherVolunteerChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to default value when ticket was created with something other than the default value" {
        var changedConfirmation: String? = ""

        val module = RevokeFieldChangesModule()
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { changedConfirmation = it; Unit.right() }
        val request = Request(0, listOf(field), emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to default value when there is a changelog entry unrelated to the field" {
        var changedConfirmation: String? = ""

        val module = RevokeFieldChangesModule()
        val changeLogItem = ChangeLogItem("Totally Not Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { changedConfirmation = it; Unit.right() }
        val request = Request(0, listOf(field), listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set to default when field was changed by a non-volunteer" {
        var changedConfirmation: String? = ""

        val module = RevokeFieldChangesModule()
        val changeLogItem = ChangeLogItem("Confirmation Status", "Confirmed", 1) { emptyList() }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { changedConfirmation = it; Unit.right() }
        val request = Request(0, listOf(field), listOf(changeLogItem)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Unconfirmed")
    }

    "should set back to status set by volunteer, when regular user changes the field" {
        var changedConfirmation: String? = ""

        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 1) { listOf("helper") }
        val userChange = ChangeLogItem("Confirmation Status", "Unconfirmed", 2) { listOf("users") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { changedConfirmation = it; Unit.right() }
        val request = Request(0, listOf(field), listOf(volunteerChange, userChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should set back to previous status when regular user changes confirmation status set by someone who no longer is a volunteer" {
        var changedConfirmation: String? = ""

        val module = RevokeFieldChangesModule()
        val volunteerChange = ChangeLogItem("Confirmation Status", "Confirmed", 0) { emptyList() }
        val userChange = ChangeLogItem("Confirmation Status", "Unconfirmed", 2) { listOf("users") }
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Unconfirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { changedConfirmation = it; Unit.right() }
        val request = Request(1, listOf(field), listOf(volunteerChange, userChange)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Confirmed")
    }

    "should return FailedModuleResponse when changing field fails" {
        val module = RevokeFieldChangesModule()
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            ""
        ) { RuntimeException().left() }
        val request = Request(0, listOf(field), emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding a comment fails" {
        val module = RevokeFieldChangesModule()
        val field = Field(
            "customfield_123",
            "Confirmation Status",
            "Confirmed",
            "Unconfirmed",
            listOf("helper"),
            "test"
        ) { Unit.right() }
        val request = Request(0, listOf(field), emptyList()) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
