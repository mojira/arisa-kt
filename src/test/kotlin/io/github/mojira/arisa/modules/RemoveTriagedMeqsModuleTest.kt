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

    "should replace only MEQS of a tag" {
        val module = RemoveTriagedMeqsModule({ _, body -> body.shouldBe("_WAI I like QC.").right() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveTriagedMeqsModuleRequest(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should not replace MEQS of tags that aren't configured" {
        val module = RemoveTriagedMeqsModule({ _, body -> body.shouldBe("_WAI I like QC.\nMEQS_TRIVIAL").right() }, listOf("MEQS_WAI"))
        val comment = mockComment("MEQS_WAI I like QC.\nMEQS_TRIVIAL")
        val request = RemoveTriagedMeqsModuleRequest(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should replace MEQS of all configured tags" {
        val module = RemoveTriagedMeqsModule(
            { _, body -> body.shouldBe("_WAI\n_WONT_FIX\nI like QC.").right() },
            listOf("MEQS_WAI", "MEQS_WONT_FIX")
        )
        val comment = mockComment("MEQS_WAI\nMEQS_WONT_FIX\nI like QC.")
        val request = RemoveTriagedMeqsModuleRequest(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun mockComment(body: String): Comment {
    val comment = mockk<Comment>()
    every { comment.body } returns body
    return comment
}
