package io.github.mojira.arisa.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.modules.commands.CommandExceptions
import io.github.mojira.arisa.modules.commands.CommandSource
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_LATER = RIGHT_NOW.plusSeconds(2)

class CommandModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no comments" {
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesn't has group staff" {
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
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
        var updatedComment = ""
        val module = CommandModule("ARISA", ::getSuccessfulDispatcher)
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe "[~arisabot]: (/) 1.\n----\nARISA_ADD_VERSION something"
    }

    "should return successfully but append the exception message when comment matches a command and it returns failed" {
        var updatedComment = ""
        val module = CommandModule("ARISA", ::getFailingDispatcher)
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe "[~arisabot]: (x) Something went wrong, but I'm too lazy to interpret the details " +
                "for you (>ω<): java.lang.RuntimeException.\n----\nARISA_ADD_VERSION something"
    }
})

private fun getComment(
    getAuthorGroups: () -> List<String> = { listOf("staff") },
    visibilityType: String? = "group",
    visibilityValue: String? = "staff",
    body: String = "ARISA_ADD_VERSION something",
    author: User = mockUser(),
    update: (String) -> Unit = {}
) = mockComment(
    created = TWO_SECONDS_LATER,
    updated = TWO_SECONDS_LATER,
    getAuthorGroups = getAuthorGroups,
    visibilityType = visibilityType,
    visibilityValue = visibilityValue,
    body = body,
    author = author,
    update = update
)

private fun getSuccessfulDispatcher(prefix: String) = CommandDispatcher<CommandSource>().apply {
    register(
        literal<CommandSource>("${prefix}_ADD_VERSION")
            .then(
                argument<CommandSource, String>("version", greedyString())
                    .executes { 1 }
            )
    )
}

private fun getFailingDispatcher(prefix: String) = CommandDispatcher<CommandSource>().apply {
    register(
        literal<CommandSource>("${prefix}_ADD_VERSION")
            .then(
                argument<CommandSource, String>("version", greedyString())
                    .executes { throw CommandExceptions.LEFT_EITHER.create(RuntimeException()) }
            )
    )
}
