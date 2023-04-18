package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.github.mojira.arisa.utils.mockProject
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val FIVE_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private val duplicatedLink = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = {
            mockIssue(
                platform = "None",
                resolution = "Duplicate",
                changeLog = listOf(duplicateLinkChangeLog)
            ).right()
        }
    )
)
private val duplicatedLink2 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = {
            mockIssue(
                platform = "Amazon",
                resolution = "Duplicate",
                changeLog = listOf(duplicateLinkChangeLog)
            ).right()
        }
    )
)
private val duplicatedLink3 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(platform = "Amazon", resolution = "Duplicate").right() }
    )
)
private val duplicatedLink4 = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = {
            mockIssue(
                platform = "Amazon",
                resolution = "Duplicate",
                changeLog = listOf(oldDuplicateLinkChangeLog)
            ).right()
        }
    )
)
private val duplicatedLinkNotResolved = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(platform = "None", resolution = null).right() }
    )
)
private val duplicateLinkChangeLog = mockChangeLogItem(
    changedToString = "This issue duplicates MC-1",
    created = RIGHT_NOW,
    field = "Link"
)
private val oldDuplicateLinkChangeLog = mockChangeLogItem(
    changedToString = "This issue duplicates MC-1",
    created = FIVE_SECONDS_AGO,
    field = "Link"
)
private val throwable = Throwable(message = "example")
private val faultyDuplicatedLink = mockLink(
    outwards = false,
    issue = mockLinkedIssue(
        getFullIssue = { throwable.left() }
    )
)
private val relatesLink = mockLink(
    type = "Relates"
)
private val duplicatesLink = mockLink()

class MultiplePlatformsModuleTest : StringSpec({
    val module = MultiplePlatformsModule(
        listOf("Xbox One", "Amazon", "Arch-Illager OS"),
        listOf("Xbox One", "Amazon", "Piglin OS"),
        listOf("Xbox One", "Amazon"),
        "Multiple",
        listOf("None"),
        "MEQS_KEEP_PLATFORM"
    )

    "should return OperationNotNeededModuleResponse when there are no links" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = emptyList()
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only Relates links" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(relatesLink)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are only outwards Duplicate links" {
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatesLink)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platforms are already set to Multiple" {
        val issue = mockIssue(
            platform = "Multiple",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the child is not resolved as Duplicate" {
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatedLinkNotResolved)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is the same as the child report" {
        val issue = mockIssue(
            platform = "Amazon",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when child report Platform is blacklisted" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(duplicatedLink)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the meqs comment is added" {
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            platform = "Xbox One",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when can't get full issue from the link" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(faultyDuplicatedLink)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(FailedModuleResponse(exceptions = listOf(throwable)))
    }

    "should return OperationNotNeededModuleResponse when Platform is null and there is a duplicate" {
        val issue = mockIssue(
            platform = null,
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is empty and there is a duplicate" {
        val issue = mockIssue(
            platform = "",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a duplicate with an old link" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(duplicatedLink4)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a duplicate without a changelog item" {
        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(duplicatedLink3)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Platform is in another project's whitelist" {
        val issue = mockIssue(
            project = mockProject(
                key = "MCPE"
            ),
            platform = "Arch-Illager OS",
            links = listOf(duplicatedLink2)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to Multiple when Platform is Xbox One and there is a duplicate" {
        var changedPlatform = ""

        val issue = mockIssue(
            platform = "Xbox One",
            links = listOf(duplicatedLink2),
            updatePlatform = { changedPlatform = it }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Multiple")
    }

    "should set to Multiple when Platform is Arch-Illager OS and there is a duplicate" {
        var changedPlatform = ""

        val issue = mockIssue(
            project = mockProject(
                key = "MCD"
            ),
            dungeonsPlatform = "Arch-Illager OS",
            links = listOf(duplicatedLink2),
            updateDungeonsPlatform = { changedPlatform = it }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Multiple")
    }

    "should set to Multiple when Platform is Piglin OS and there is a duplicate" {
        var changedPlatform = ""

        val issue = mockIssue(
            project = mockProject(
                key = "MCLG"
            ),
            legendsPlatform = "Piglin OS",
            links = listOf(duplicatedLink2),
            updateLegendsPlatform = { changedPlatform = it }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Multiple")
    }
})
