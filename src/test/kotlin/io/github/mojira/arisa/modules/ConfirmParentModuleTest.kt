package io.github.mojira.arisa.modules

import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ConfirmParentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when Linked is null" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Unconfirmed", null) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Linked is 0" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Unconfirmed", 0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Community Consensus when Confirmation Status is unset and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request(null, 1) {
            changedConfirmation = it
            Unit.right()
        }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Unconfirmed and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Unconfirmed", 1) {
            changedConfirmation = it
            Unit.right()
        }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Plausible and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Plausible", 1) {
            changedConfirmation = it
            Unit.right()
        }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is Community Consensus and Linked is 1" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Community Consensus", 1) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is Confirmed and Linked is 1" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus")
        val request = ConfirmParentModule.Request("Community Consensus", 1) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
