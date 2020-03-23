package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.should
import io.kotlintest.shouldBe
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

    "should return FailedModuleResponse when deleting fails" {
        val module = AttachmentModule({ RuntimeException().left() }, listOf(".test"))
        val attachment = mockAttachment("testfile.test")
        val request = AttachmentModuleRequest(listOf(attachment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when deleting fails" {
        val module = AttachmentModule({ RuntimeException().left() }, listOf(".test"))
        val attachment = mockAttachment("testfile.test")
        val request = AttachmentModuleRequest(listOf(attachment, attachment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return ModuleResponse when something is deleted succesfully" {
        val module = AttachmentModule({ Unit.right() }, listOf(".test"))
        val attachment = mockAttachment("testfile.test")
        val request = AttachmentModuleRequest(listOf(attachment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun mockAttachment(name: String): Attachment {
    val attachment = mockk<Attachment>()
    every { attachment.contentUrl } returns name
    return attachment
}
