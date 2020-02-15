package io.github.mojira.arisa.modules

import arrow.core.right
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Attachment

class AttachmentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no attachments" {
        val module = AttachmentModule({ Unit.right() }, emptyList())
        val request = AttachmentModuleRequest(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no blacklisted attachments" {
        val module = AttachmentModule({ Unit.right() }, listOf(".test"))
        val attachment = mockAttachment("testfile")

        val request = AttachmentModuleRequest(listOf(attachment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})

fun mockAttachment(name: String): Attachment {
    val attachment = mockk<Attachment>()
    every { attachment.contentUrl } returns name
    return attachment
}
