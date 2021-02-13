package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.modules.commands.Command1
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_LATER = RIGHT_NOW.plusSeconds(2)

class CommandModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no comments" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesn't has group staff" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            getAuthorGroups = { emptyList() }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment has group users" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            visibilityType = "group",
            visibilityValue = "users"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment doesnt have correct group" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            visibilityType = "notagroup"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment doesnt have correct value" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            visibilityValue = "notagroup"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is not restricted" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            visibilityType = null,
            visibilityValue = null
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment doesnt start with ARISA_" {
        val module = CommandModule(mockOperationNotNeededCommand, mockUnitCommand)
        val comment = getComment(
            body = "ARISA"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment doesnt have spaces" {
        val module = CommandModule(mockUnitCommand, mockUnitCommand)
        val comment = getComment(
            body = "ARISA_ADD_VERSION"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return successfully when comment matches a command and it returns successfully" {
        var addedRawComment = ""
        val module = CommandModule(mockOperationNotNeededCommand, mockUnitCommand)
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            )
        )
        val issue = mockIssue(
            comments = listOf(comment),
            addRawRestrictedComment = { addedComment, _ -> addedRawComment = addedComment }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        addedRawComment shouldBe "Command execution was successful.\n~Author: SPTesting~"
    }

    "should return OperationNotNeededModuleResponse when comment matches a command and it returns OperationNotNeededModuleResponse" {
        val module = CommandModule(mockOperationNotNeededCommand, mockOperationNotNeededCommand)
        val comment = getComment()
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return failed when comment matches a command and it returns failed" {
        var addedRawComment = ""
        val module = CommandModule(mockOperationNotNeededCommand, mockFailingCommand)
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            )
        )
        val issue = mockIssue(
            comments = listOf(comment),
            addRawRestrictedComment = { addedComment, _ -> addedRawComment = addedComment }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
        addedRawComment shouldBe "Command execution failed.\n~Author: SPTesting~"
    }
})

private fun getComment(
    getAuthorGroups: () -> List<String> = { listOf("staff") },
    visibilityType: String? = "group",
    visibilityValue: String? = "staff",
    body: String = "ARISA_ADD_VERSION something",
    author: User = mockUser()
) = mockComment(
    created = TWO_SECONDS_LATER,
    updated = TWO_SECONDS_LATER,
    getAuthorGroups = getAuthorGroups,
    visibilityType = visibilityType,
    visibilityValue = visibilityValue,
    body = body,
    author = author
)

val mockUnitCommand = object : Command1 {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Unit.right()
}

val mockFailingCommand = object : Command1 {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> =
        FailedModuleResponse(
            listOf(RuntimeException())
        ).left()
}

val mockOperationNotNeededCommand = object : Command1 {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> =
        OperationNotNeededModuleResponse.left()
}
