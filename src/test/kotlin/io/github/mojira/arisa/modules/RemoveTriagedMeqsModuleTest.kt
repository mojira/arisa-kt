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
import net.rcarz.jiraclient.Comment

class RemoveTriagedMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no priority and no triaged time" {
        val module = RemoveTriagedMeqsModule({ _, _ -> Unit.right() }, emptyList())
        val request = RemoveTriagedMeqsModuleRequest(null, null, emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveTriagedMeqsModule({ _, _ -> Unit.right() }, emptyList())
        val request = RemoveTriagedMeqsModuleRequest("Important", "triaged", emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveTriagedMeqsModule({ _, _ -> Unit.right() }, listOf("MEQS_WAI"))
        val comment = mockComment("I like QC.")
        val request = RemoveTriagedMeqsModuleRequest("Important", "triaged", listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveTriagedMeqsModule({ _, _ -> RuntimeException().left() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveTriagedMeqsModuleRequest("Important", "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveTriagedMeqsModule({ _, _ -> RuntimeException().left() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveTriagedMeqsModuleRequest("Important", "triaged", listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should process tickets with Mojang Priority" {
        val module = RemoveTriagedMeqsModule({ _, _ -> Unit.right() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveTriagedMeqsModuleRequest("Important", null, listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should process tickets with triaged time" {
        val module = RemoveTriagedMeqsModule({ _, _ -> Unit.right() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveTriagedMeqsModuleRequest(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

fun mockComment(body: String): Comment {
    val comment = mockk<Comment>()
    every { comment.body } returns body
    return comment
}
