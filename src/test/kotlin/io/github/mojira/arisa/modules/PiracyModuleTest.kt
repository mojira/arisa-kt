package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_LATER = RIGHT_NOW.plusSeconds(2)

class PiracyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when the ticket was created after last run" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "test"
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue()

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            environment = "",
            summary = "",
            description = ""
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when piracy signatures is empty" {
        val module = PiracyModule(emptyList(), "message")
        val issue = mockIssue(
            environment = "",
            summary = "",
            description = "test"
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no signature matches" {
        val module = PiracyModule(emptyList(), "message")
        val issue = mockIssue(
            environment = "else",
            summary = "nope",
            description = "something"
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if description contains a piracy signature" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            environment = "",
            summary = "",
            description = "test"
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if description contains a piracy signature but not as a full word" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            environment = "",
            summary = "",
            description = "testusername"
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if summary contains a piracy signature" {
        var hasCommented = false

        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            environment = "",
            summary = "test",
            description = "",
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }

    "should resolve as invalid if environment contains a piracy signature" {
        var hasCommented = false

        val module = PiracyModule(listOf("test"), "message")
        val issue = mockIssue(
            environment = "test",
            summary = "",
            description = "",
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }

    "should resolve as invalid if environment contains a piracy signature using whitespaces" {
        var hasCommented = false

        val module = PiracyModule(listOf("signature with whitespaces"), "message")
        val issue = mockIssue(
            environment = "signature with whitespaces",
            summary = "",
            description = "",
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = module(issue, TWO_SECONDS_LATER)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }
})
