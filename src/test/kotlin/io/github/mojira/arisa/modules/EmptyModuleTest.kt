package io.github.mojira.arisa.modules

import arrow.core.left
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class EmptyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when ticket was created before the last run" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = A_SECOND_AGO
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and desc and env are correct" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            attachments = listOf(mockAttachment()),
            description = "asddsa",
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc and env are correct" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "asddsa",
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is an attachment and no desc or env" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            attachments = listOf(mockAttachment())
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and no desc and env is correct" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc is correct and no env" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "asdasd"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when there is no attachment and no desc or env" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is default and env is empty" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = DESC_DEFAULT
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is empty and env is default" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            environment = ENV_DEFAULT
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is too short and env is too short" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "asd",
            environment = "asd"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = EmptyModule("message")
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "asd",
            environment = "asd",
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
