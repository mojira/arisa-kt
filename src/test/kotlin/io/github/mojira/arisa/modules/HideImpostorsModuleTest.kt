package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.utils.NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no comments" {
        val module = HideImpostorsModule()
        val issue = mockIssue()

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

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

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is more than a day old" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            getAuthorGroups = { listOf("staff") },
            created = NOW.minus(2, ChronoUnit.DAYS)
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should hide comment when user starts with a valid tag but is not of a permission group" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains numbers" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[t3st] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains accented letter" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[tést] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains spaces" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[Mojang Overlord] test"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to a group" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            visibilityType = "not a group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to the correct group" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            visibilityType = "group",
            visibilityValue = "users"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when hiding the comment fails" {
        val module = HideImpostorsModule()
        val comment = getComment(
            author = "[test] test",
            restrict = { RuntimeException().left() }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getUser(displayName: String) = User("", displayName)

private fun getComment(
    author: String = "User",
    getAuthorGroups: () -> List<String> = { emptyList() },
    created: Instant = NOW,
    visibilityType: String? = null,
    visibilityValue: String? = null,
    restrict: (String) -> Either<Throwable, Unit> = { Unit.right() }
) = mockComment(
    author = getUser(displayName = author),
    getAuthorGroups = getAuthorGroups,
    created = created,
    updated = created,
    visibilityType = visibilityType,
    visibilityValue = visibilityValue,
    restrict = restrict
)
