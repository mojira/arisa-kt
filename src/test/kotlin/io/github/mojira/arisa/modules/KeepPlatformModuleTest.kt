package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val CHANGE_PLATFORM = mockChangeLogItem(
    created = RIGHT_NOW.minusSeconds(10),
    field = "platform",
    changedFromString = "Amazon"
)

class KeepPlatformModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val issue = mockIssue(
            changeLog = listOf(CHANGE_PLATFORM)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains platform tag" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "Hello world!",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(CHANGE_PLATFORM)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment isn't restricted to staff group" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(CHANGE_PLATFORM)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket’s platform was changed by staff" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val issue = mockIssue(
            comments = listOf(comment),
            platform = "Xbox One",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should set back to platform set by volunteer, when regular user changes platform" {
        var changedPlatform = ""

        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val volunteerChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val userChange = mockChangeLogItem(value = "Unconfirmed") { listOf("users") }
        val issue = mockIssue(
            comments = listOf(comment),
            platform = "None",
            changeLog = listOf(volunteerChange, userChange),
            updatePlatforms = { changedPlatform = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Amazon")
    }
})

private fun mockChangeLogItem(
    created: Instant = RIGHT_NOW,
    field: String = "Platform",
    value: String = "Amazon",
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = mockChangeLogItem(
    created = created,
    field = field,
    changedToString = value,
    getAuthorGroups = getAuthorGroups
)
