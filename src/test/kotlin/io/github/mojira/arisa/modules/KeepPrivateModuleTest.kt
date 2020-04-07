package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Comment

class KeepPrivateModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPrivateModule({ Unit.right() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest(null, "private", emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains private tag" {
        val module = KeepPrivateModule({ Unit.right() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("Hello world!")
        val request = KeepPrivateModuleRequest(null, "private", listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when security level is set to private" {
        val module = KeepPrivateModule({ Unit.right() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest("private", "private", listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to private when security level is null" {
        val module = KeepPrivateModule({ Unit.right() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest(null, "private", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should set to private when security level is not private" {
        val module = KeepPrivateModule({ Unit.right() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest("not private", "private", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when setting security level fails" {
        val module = KeepPrivateModule({ RuntimeException().left() }, { Unit.right() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest(null, "private", listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when posting comment" {
        val module = KeepPrivateModule({ Unit.right() }, { RuntimeException().left() }, "MEQS_KEEP_PRIVATE")
        val comment = mockComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModuleRequest(null, "private", listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun mockComment(body: String): Comment {
    val comment = mockk<Comment>()
    every { comment.body } returns body
    return comment
}
