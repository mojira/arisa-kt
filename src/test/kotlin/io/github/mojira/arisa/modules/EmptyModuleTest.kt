package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.getAttachment
import io.github.mojira.arisa.utils.getIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class EmptyModuleTest : StringSpec({
    val A_SECOND_AGO = NOW.minusSeconds(1)

    "should return OperationNotNeededModuleResponse when ticket was created before the last run" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = A_SECOND_AGO
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and desc and env are correct" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            attachments = listOf(getAttachment()),
            description = "asddsa",
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc and env are correct" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            description = "asddsa",
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and no desc or env" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            attachments = listOf(getAttachment())
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and no desc and env is correct" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            environment = "asddsa"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc is correct and no env" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            description = "asdasd"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when there is no attachment and no desc or env" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is default and env is empty" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            description = DESC_DEFAULT
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is empty and env is default" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            environment = ENV_DEFAULT
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is too short and env is too short" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            description = "asd",
            environment = "asd"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving fails" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
            description = "asd",
            environment = "asd",
            resolveAsIncomplete = { RuntimeException().left() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = EmptyModule("message")
        val issue = getIssue(
            created = NOW,
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
