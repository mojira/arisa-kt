package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.modules.AttachmentModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class AttachmentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no attachments" {
        val module = AttachmentModule(emptyList())
        val request = Request(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no blacklisted attachments" {
        val module = AttachmentModule(listOf(".test"))
        val attachment = Attachment("testfile") { Unit.right() }
        val request = Request(listOf(attachment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when deleting fails" {
        val module = AttachmentModule(listOf(".test"))
        val attachment = Attachment("testfile.test") { RuntimeException().left() }
        val request = Request(listOf(attachment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when deleting fails" {
        val module = AttachmentModule(listOf(".test"))
        val attachment = Attachment("testfile.test") { RuntimeException().left() }
        val request = Request(listOf(attachment, attachment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return ModuleResponse when something is deleted succesfully" {
        val module = AttachmentModule(listOf(".test"))
        val attachment = Attachment("testfile.test") { Unit.right() }
        val request = Request(listOf(attachment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
