package io.github.mojira.arisa.modules

import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import java.time.Instant

private val NOW = Instant.now()

class AttachmentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no attachments" {
        val module = AttachmentModule(emptyList())
        val issue = mockIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no blacklisted attachments" {
        val module = AttachmentModule(listOf(".test"))
        val attachment = getAttachment(
            name = "testfile"
        )
        val issue = mockIssue(
            attachments = listOf(attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when something is deleted successfully" {
        val module = AttachmentModule(listOf(".test"))
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
    remove: () -> Unit = { Unit }
) = Attachment(
    name,
    created,
    remove,
    { ByteArray(0) }
)
