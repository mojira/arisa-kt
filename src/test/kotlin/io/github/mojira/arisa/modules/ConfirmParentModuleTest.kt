package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.getIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class ConfirmParentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when Linked is null" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Unconfirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Linked is 0" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Unconfirmed",
            linked = 0.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is Community Consensus and Linked is 1" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Community Consensus",
            linked = 1.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is Confirmed and Linked is 1" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Community Consensus",
            linked = 1.0
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Community Consensus when Confirmation Status is null and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = null,
            linked = 1.0,
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is empty and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "",
            linked = 1.0,
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Unconfirmed and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Unconfirmed",
            linked = 1.0,
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Plausible and Linked is 1" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 1.0)
        val issue = getIssue(
            confirmationStatus = "Plausible",
            linked = 1.0,
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }
})
