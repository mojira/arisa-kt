package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.getIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class PiracyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "",
            summary = "",
            description = ""
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when piracy signatures is empty" {
        val module = PiracyModule(emptyList(), "message")
        val issue = getIssue(
            environment = "",
            summary = "",
            description = "test"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no signature matches" {
        val module = PiracyModule(emptyList(), "message")
        val issue = getIssue(
            environment = "else",
            summary = "nope",
            description = "something"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if description contains a piracy signature" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "",
            summary = "",
            description = "test"
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if description contains a piracy signature but not as a full word" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "",
            summary = "",
            description = "testusername"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if summary contains a piracy signature" {
        var hasCommented = false

        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "",
            summary = "test",
            description = "",
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }

    "should resolve as invalid if environment contains a piracy signature" {
        var hasCommented = false

        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "test",
            summary = "",
            description = ""
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }

    "should resolve as invalid if environment contains a piracy signature using whitespaces" {
        var hasCommented = false

        val module = PiracyModule(listOf("signature with whitespaces"), "message")
        val issue = getIssue(
            environment = "signature with whitespaces",
            summary = "",
            description = "",
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        hasCommented shouldBe true
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "test",
            summary = "",
            description = "",
            resolveAsInvalid = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = PiracyModule(listOf("test"), "message")
        val issue = getIssue(
            environment = "test",
            summary = "",
            description = "",
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
