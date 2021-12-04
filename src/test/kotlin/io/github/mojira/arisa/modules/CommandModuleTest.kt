package io.github.mojira.arisa.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.infrastructure.AttachmentUtils
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

private const val PREFIX = "ARISA"
private const val BOT_USER_NAME = "botName"

class CommandModuleTest : StringSpec({
    val attachmentUtils = AttachmentUtils(emptyList(), CrashReader(), BOT_USER_NAME)
    val module = CommandModule(PREFIX, BOT_USER_NAME, true, attachmentUtils, ::getDispatcher)

    "should return OperationNotNeededModuleResponse when no comments" {
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesn't has group staff" {
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

    "should return OperationNotNeededModuleResponse when comment doesn't have correct group" {
        val comment = getComment(
            visibilityType = "notagroup"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment doesn't have correct value" {
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

    "should return OperationNotNeededModuleResponse when comment has no underscore after prefix" {
        val comment = getComment(
            // No underscore ('_') after prefix
            body = PREFIX
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment has been written by bot" {
        val comment = getComment(
            author = mockUser(
                name = BOT_USER_NAME
            )
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return successfully when comment matches a command and it returns successfully" {
        var updatedComment = ""
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
        updatedComment shouldBe """
            |(/) ARISA_SUCCESS arg
            |→ {{Command was executed successfully, with 1 affected element(s)}} ~??[~botName]??~
        """.trimMargin()
    }

    "should return with given return value when command executes successfully" {
        var updatedComment = ""
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it },
            body = "ARISA_VALUE 42"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe """
            |(/) ARISA_VALUE 42
            |→ {{Command was executed successfully, with 42 affected element(s)}} ~??[~botName]??~
        """.trimMargin()
    }

    "should return successfully but append the exception message when comment matches a command and it returns failed" {
        var updatedComment = ""
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it },
            body = "ARISA_FAIL arg"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe """
            |(x) ARISA_FAIL arg
            |→ {{Testing error message: java.lang.RuntimeException}} ~??[~botName]??~
        """.trimMargin()
    }

    "should work for other prefixes" {
        var updatedComment = ""
        @Suppress("NAME_SHADOWING")
        val module = CommandModule("TESTING_COMMAND", "userName", true, attachmentUtils, ::getDispatcher)
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it },
            body = "TESTING_COMMAND_SUCCESS arg"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe """
            |(/) TESTING_COMMAND_SUCCESS arg
            |→ {{Command was executed successfully, with 1 affected element(s)}} ~??[~userName]??~
        """.trimMargin()
    }

    "should be able to handle multiple commands in the same comment" {
        var updatedComment = ""
        val comment = getComment(
            author = mockUser(
                name = "SPTesting"
            ),
            update = { updatedComment = it },
            body = """
                |Hello Arisa please do the following commands
                |ARISA_SUCCESS arg1
                |ARISA_FAIL arg2
                |and this one
                |ARISA_SUCCESS arg3
                |Thank you!
            """.trimMargin()
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight()
        updatedComment shouldBe """
            |Hello Arisa please do the following commands
            |
            |(/) ARISA_SUCCESS arg1
            |→ {{Command was executed successfully, with 1 affected element(s)}} ~??[~botName]??~
            |
            |(x) ARISA_FAIL arg2
            |→ {{Testing error message: java.lang.RuntimeException}} ~??[~botName]??~
            |
            |and this one
            |
            |(/) ARISA_SUCCESS arg3
            |→ {{Command was executed successfully, with 1 affected element(s)}} ~??[~botName]??~
            |
            |Thank you!
        """.trimMargin()
    }
})

private fun getComment(
    getAuthorGroups: () -> List<String> = { listOf("staff") },
    visibilityType: String? = "group",
    visibilityValue: String? = "staff",
    body: String = "${PREFIX}_SUCCESS arg",
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

private fun getDispatcher(prefix: String) = CommandDispatcher<CommandSource>().apply {
    register(
        literal<CommandSource>("${prefix}_SUCCESS")
            .then(
                argument<CommandSource, String>("arg", greedyString())
                    .executes { 1 }
            )
    )
    register(
        literal<CommandSource>("${prefix}_VALUE")
            .then(
                argument<CommandSource, Int>("arg", integer())
                    .executes { it.getArgument("arg", Int::class.java) }
            )
    )
    register(
        literal<CommandSource>("${prefix}_FAIL")
            .then(
                argument<CommandSource, String>("arg", greedyString())
                    .executes { throw CommandExceptions.TEST_EXCEPTION.create(RuntimeException()) }
            )
    )
}
