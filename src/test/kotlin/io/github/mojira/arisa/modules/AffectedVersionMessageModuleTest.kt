package io.github.mojira.arisa.modules

import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import java.time.Instant
import java.time.temporal.ChronoUnit

private val YESTERDAY: Instant = RIGHT_NOW.minus(1, ChronoUnit.DAYS)
private val VERSION_1 = mockVersion(
    id = "1"
)
private val VERSION_2 = mockVersion(
    id = "2"
)

class AffectedVersionMessageModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when version is not in map" {
        val module = AffectedVersionMessageModule(emptyMap())
        val issue = mockIssue(
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, YESTERDAY)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when report was created before last run" {
        val module = AffectedVersionMessageModule(mapOf(
            VERSION_1.id to "message-1"
        ))
        val issue = mockIssue(
            created = YESTERDAY,
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when report was created by staff" {
        val module = AffectedVersionMessageModule(mapOf(
            VERSION_1.id to "message-1"
        ))
        val issue = mockIssue(
            reporter = mockUser(
                getGroups = { listOf("staff") }
            ),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, YESTERDAY)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add message when issue has affected version with message" {
        val module = AffectedVersionMessageModule(mapOf(
            VERSION_1.id to "message-1"
        ))
        val addedMessages = mutableListOf<CommentOptions>()

        val issue = mockIssue(
            affectedVersions = listOf(VERSION_1),
            addComment = addedMessages::add
        )

        val result = module(issue, YESTERDAY)
        result.shouldBeRight(ModuleResponse)
        addedMessages shouldContainExactly listOf(CommentOptions("message-1"))
    }

    "should add only one message" {
        val module = AffectedVersionMessageModule(mapOf(
            VERSION_1.id to "message-1",
            VERSION_2.id to "message-2"
        ))
        val addedMessages = mutableListOf<CommentOptions>()

        val issue = mockIssue(
            affectedVersions = listOf(VERSION_1, VERSION_2),
            addComment = addedMessages::add
        )

        val result = module(issue, YESTERDAY)
        result.shouldBeRight(ModuleResponse)

        // Should only contain 1 message (but does not matter which one)
        addedMessages shouldHaveSize 1
        addedMessages shouldContainAnyOf listOf(
            CommentOptions("message-1"),
            CommentOptions("message-2")
        )
    }
})
