package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val duplicatedLink1 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(reporter = mockUser(name = "user1")).right() }
    )
)
private val duplicatedLink2 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(reporter = mockUser(name = "user2")).right() }
    )
)
private val duplicatedLinkError = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { RuntimeException().left() }
    )
)
private val relatesLink = mockLink(
    type = "Relates"
)
private val duplicatesLink = mockLink()

class ConfirmParentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no links" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            links = emptyList()
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only Relates links" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            links = listOf(relatesLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only outwards Duplicate links" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            links = listOf(duplicatesLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is already Community Consensus" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Community Consensus",
            links = listOf(duplicatedLink1, duplicatedLink2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Confirmation Status is already Confirmed" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Confirmed",
            links = listOf(duplicatedLink1, duplicatedLink2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the duplicated issues are all of the same reporter" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            links = listOf(duplicatedLink1, duplicatedLink1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when one of the duplicated issue has the same reporter as parent" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            reporter = mockUser(name = "user1"),
            confirmationStatus = "Unconfirmed",
            links = listOf(duplicatedLink1, duplicatedLink2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Community Consensus when Confirmation Status is null and there are enough duplicates" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = null,
            links = listOf(duplicatedLink1, duplicatedLink2, duplicatedLinkError),
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is empty and there are enough duplicates" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "",
            links = listOf(duplicatedLink1, duplicatedLink2, duplicatedLinkError),
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Unconfirmed and there are enough duplicates" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed",
            links = listOf(duplicatedLink1, duplicatedLink2, duplicatedLinkError),
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should set to Community Consensus when Confirmation Status is Plausible and there are enough duplicates" {
        var changedConfirmation = ""

        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = "Plausible",
            links = listOf(duplicatedLink1, duplicatedLink2, duplicatedLinkError),
            updateConfirmationStatus = {
                changedConfirmation = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedConfirmation.shouldBe("Community Consensus")
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = ConfirmParentModule(listOf("Unconfirmed", "Plausible"), "Community Consensus", 2.0)
        val issue = mockIssue(
            confirmationStatus = null,
            links = listOf(duplicatedLinkError)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
