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
private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)
private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val THREE_SECONDS_AGO = RIGHT_NOW.minusSeconds(3)

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

    "should return OperationNotNeededModuleResponse when Ticket’s platform was last changed by staff after the comment" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM",
            visibilityType = "group",
            visibilityValue = "staff",
            created = TWO_SECONDS_AGO
        )
        val userChange = mockPlatformChangeLogItem(
            oldValue = "None",
            newValue = "Android",
            created = A_SECOND_AGO
        ) { listOf("users") }
        val volunteerChange = mockPlatformChangeLogItem(created = RIGHT_NOW) { listOf("staff") }
        val issue = mockIssue(
            comments = listOf(comment),
            platform = "Amazon",
            changeLog = listOf(userChange, volunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket’s platform was last changed by staff before the comment" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
                body = "MEQS_KEEP_PLATFORM",
                visibilityType = "group",
                visibilityValue = "staff",
                created = RIGHT_NOW
        )
        val volunteerChange = mockPlatformChangeLogItem(created = A_SECOND_AGO) { listOf("staff") }
        val issue = mockIssue(
                comments = listOf(comment),
                platform = "Amazon",
                changeLog = listOf(volunteerChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket’s platform was changed to the same value as last volunteer change after the comment" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
                body = "MEQS_KEEP_PLATFORM",
                visibilityType = "group",
                visibilityValue = "staff",
                created = THREE_SECONDS_AGO
        )
        val volunteerChange = mockPlatformChangeLogItem(created = TWO_SECONDS_AGO) { listOf("staff") }
        val userChange1 = mockPlatformChangeLogItem(
                created = A_SECOND_AGO,
                oldValue = "Amazon",
                newValue = "Android"
        ) { listOf("users") }
        val userChange2 = mockPlatformChangeLogItem(created = RIGHT_NOW) { listOf("users") }
        val issue = mockIssue(
                comments = listOf(comment),
                platform = "Amazon",
                changeLog = listOf(volunteerChange, userChange1, userChange2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket’s platform was changed to the same value as previous state at the time of the comment" {
        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
                body = "MEQS_KEEP_PLATFORM",
                visibilityType = "group",
                visibilityValue = "staff",
                created = TWO_SECONDS_AGO
        )
        val userChange1 = mockPlatformChangeLogItem(
                created = A_SECOND_AGO,
                oldValue = "Amazon",
                newValue = "Android"
        ) { listOf("users") }
        val userChange2 = mockPlatformChangeLogItem(created = RIGHT_NOW) { listOf("users") }
        val issue = mockIssue(
                comments = listOf(comment),
                platform = "Amazon",
                changeLog = listOf(userChange1, userChange2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set back to platform set by volunteer after the comment, when regular user changes platform" {
        var changedPlatform = ""

        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
            body = "MEQS_KEEP_PLATFORM",
            visibilityType = "group",
            visibilityValue = "staff",
            created = TWO_SECONDS_AGO
        )
        val volunteerChange = mockPlatformChangeLogItem(created = A_SECOND_AGO) { listOf("staff") }
        val userChange = mockPlatformChangeLogItem(created = RIGHT_NOW, newValue = "None") { listOf("users") }
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

    "should set back to platform that was set last before the comment, when regular user changes platform and there was no change by volunteer" {
        var changedPlatform = ""

        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
                body = "MEQS_KEEP_PLATFORM",
                visibilityType = "group",
                visibilityValue = "staff",
                created = TWO_SECONDS_AGO
        )
        val userChange = mockPlatformChangeLogItem(newValue = "None", created = A_SECOND_AGO) { listOf("users") }
        val issue = mockIssue(
                comments = listOf(comment),
                platform = "None",
                changeLog = listOf(userChange),
                updatePlatforms = { changedPlatform = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Android")
    }

    "should set back to platform that was set last before the comment, when regular user changes platform and the last change by volunteer was before the comment" {
        var changedPlatform = ""

        val module = KeepPlatformModule("MEQS_KEEP_PLATFORM")
        val comment = mockComment(
                body = "MEQS_KEEP_PLATFORM",
                visibilityType = "group",
                visibilityValue = "staff",
                created = A_SECOND_AGO
        )
        val volunteerChange = mockPlatformChangeLogItem(created = THREE_SECONDS_AGO) { listOf("staff") }
        val userChange1 = mockPlatformChangeLogItem(
                created = TWO_SECONDS_AGO,
                oldValue = "Amazon",
                newValue = "Android"
        ) { listOf("users") }
        val userChange2 = mockPlatformChangeLogItem(created = RIGHT_NOW, newValue = "None") { listOf("users") }
        val issue = mockIssue(
                comments = listOf(comment),
                platform = "None",
                changeLog = listOf(volunteerChange, userChange1, userChange2),
                updatePlatforms = { changedPlatform = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPlatform.shouldBe("Android")
    }
})

private fun mockPlatformChangeLogItem(
    created: Instant = RIGHT_NOW,
    field: String = "Platform",
    oldValue: String = "Android",
    newValue: String = "Amazon",
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = mockChangeLogItem(
    created = created,
    field = field,
    changedFromString = oldValue,
    changedToString = newValue,
    getAuthorGroups = getAuthorGroups
)
