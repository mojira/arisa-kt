package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no comments" {
        val module = HideImpostorsModule()
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain [ but contains ]" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "test] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain ] but contains [" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains invalid characters in the tag" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[}[{]] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user is only the tag" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test]"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when tag is not at the beginning" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "test [test]"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group staff" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            getAuthorGroups = { listOf("staff") }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group helper" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            getAuthorGroups = { listOf("helper") }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group global-moderators" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            getAuthorGroups = { listOf("global-moderators") }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is hidden" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is more than a day old" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            getAuthorGroups = { listOf("staff") },
            created = RIGHT_NOW.minus(2, ChronoUnit.DAYS)
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment was edited by a staff+ user" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            updateAuthor = "[Mod] Moderator",
            getAuthorGroups = { listOf("user") },
            getUpdateAuthorGroups = { listOf("staff") },
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isRestricted.shouldBeFalse()
    }

    "should hide comment when user starts with a valid tag but is not of a permission group" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should hide comment when tag contains numbers" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[t3st] test",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should hide comment when tag contains accented letter" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[tést] test",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should hide comment when tag contains spaces" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[Mojang Overlord] test",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to a group" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            visibilityType = "not a group",
            visibilityValue = "staff",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to the correct group" {
        var isRestricted = false
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            visibilityType = "group",
            visibilityValue = "users",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }
})

private fun getUser(displayName: String) = mockUser(name = "", displayName = displayName)

private fun getComment(
    author: String = "User",
    updateAuthor: String? = null,
    getAuthorGroups: () -> List<String> = { emptyList() },
    getUpdateAuthorGroups: () -> List<String> = { emptyList() },
    created: Instant = RIGHT_NOW,
    visibilityType: String? = null,
    visibilityValue: String? = null,
    restrict: (String) -> Unit = { }
) = mockComment(
    author = getUser(displayName = author),
    updateAuthor = if (updateAuthor == null) null else getUser(displayName = updateAuthor),
    getAuthorGroups = getAuthorGroups,
    getUpdateAuthorGroups = getUpdateAuthorGroups,
    created = created,
    updated = created,
    visibilityType = visibilityType,
    visibilityValue = visibilityValue,
    restrict = restrict
)
