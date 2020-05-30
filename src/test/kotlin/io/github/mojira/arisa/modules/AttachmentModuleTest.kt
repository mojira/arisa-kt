package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class AttachmentModuleTest : StringSpec({

    "should return OperationNotNeededModuleResponse when there is no attachments" {
        val module = AttachmentModule(emptyList(), "attach-new-attachment")
        val issue = mockIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no blacklisted attachments" {
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            name = "testfile"
        )
        val issue = mockIssue(
            attachments = listOf(attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when deleting fails" {
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            remove = { RuntimeException().left() }
        )
        val issue = mockIssue(
            attachments = listOf(attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when deleting fails" {
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            remove = { RuntimeException().left() }
        )
        val issue = mockIssue(
            attachments = listOf(attachment, attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return ModuleResponse when something is deleted successfully" {
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment()
        val issue = mockIssue(
            attachments = listOf(attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun getAttachment(
    name: String = "testfile.test",
    created: Instant = NOW,
    remove: () -> Either<Throwable, Unit> = { Unit.right() }
) = Attachment(
    name,
    created,
    remove,
    { ByteArray(0) }
)
