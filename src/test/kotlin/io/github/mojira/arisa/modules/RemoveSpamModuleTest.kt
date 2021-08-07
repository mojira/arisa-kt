package io.github.mojira.arisa.modules

import io.github.mojira.arisa.infrastructure.config.SpamPatternConfig
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.time.Instant
import java.time.temporal.ChronoUnit

private val YESTERDAY: Instant = RIGHT_NOW.minus(1, ChronoUnit.DAYS)

class RemoveSpamModuleTest : StringSpec({
    val module = RemoveSpamModule(listOf(
        SpamPatternConfig("SPAM", 3)
    ))

    "should return OperationNotNeededModuleResponse when no comments" {
        val issue = mockIssue()

        val result = module(issue, YESTERDAY)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModule when comment was posted before the last run" {
        val comment = mockComment(
            created = RIGHT_NOW.minus(1, ChronoUnit.SECONDS)
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModule when comment is from a volunteer" {
        val comment = mockComment(
            getAuthorGroups = { listOf("helper") }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, YESTERDAY)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should hide comment when it triggers a pattern" {
        var isRestricted = false
        val comment = mockComment(
            body = "HEY THIS IS AN ANNOYING COMMENT SPAM SPAM SPAM",
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, YESTERDAY)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should return OperationNotNeededModule when comment contains pattern not often enough" {
        val comment = mockComment(
            body = "this SPAM is getting annoying"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, YESTERDAY)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should hide comment when pattern is triggered very often" {
        var isRestricted = false
        val comment = mockComment(
            body = """EGG AND BACON
                |EGG, SAUSAGE AND BACON
                |EGG AND SPAM
                |EGG, BACON AND SPAM
                |EGG, BACON, SAUSAGE AND SPAM
                |SPAM, BACON, SAUSAGE AND SPAM
                |SPAM, EGG, SPAM, SPAM, BACON AND SPAM
                |SPAM, SPAM, SPAM, EGG AND SPAM
                |SPAM SAUSAGE, SPAM, SPAM, SPAM, BACON, SPAM, TOMATO AND SPAM
                |SPAM, SPAM, SPAM, SPAM, SPAM, SPAM, BAKED BEANS, SPAM, SPAM, SPAM AND SPAM
                |LOBSTER THERMIDOR AUX CREVETTES WITH A MORNAY SAUCE, GARNISHED WITH TRUFFLE PÂTÉ, BRANDY AND A FRIED EGG ON TOP, AND SPAM""".trimMargin(),
            restrict = { isRestricted = true }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, YESTERDAY)

        result.shouldBeRight(ModuleResponse)
        isRestricted.shouldBeTrue()
    }

    "should return OperationNotNeededModule when comment is already restricted" {
        val comment = mockComment(
            body = "HEY THIS IS AN ANNOYING COMMENT SPAM SPAM SPAM",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, YESTERDAY)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
