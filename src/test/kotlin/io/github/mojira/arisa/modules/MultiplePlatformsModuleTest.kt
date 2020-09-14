package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val duplicatedLink = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(platform = "None").right() }
    )
)
private val duplicatedLink2 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(platform = "Amazon").right() }
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

class MultiplePlatformsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no links" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Xbox One",
            links = emptyList()
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only Relates links" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(relatesLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only outwards Duplicate links" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatesLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platforms are already set to Multiple" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Multiple",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is the same as the child report" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when child report Platform is blacklisted" {
        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatedLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is null and there is a duplicate" {
        var changedPlatform = ""

        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = null,
            links = listOf(duplicatedLink2),
            updatePlatforms = {
                changedPlatform = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is empty and there is a duplicate" {
        var changedPlatform = ""

        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "",
            links = listOf(duplicatedLink2),
            updatePlatforms = {
                changedPlatform = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Multiple when Platform is Xbox One and there is a duplicate" {
        var changedPlatform = ""

        val module = MultiplePlatformsModule(listOf("Xbox One", "Amazon"), "Multiple", listOf("None"))
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(duplicatedLink2),
            updatePlatforms = {
                changedPlatform = it
                Unit.right()
            }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Multiple")
    }
})
