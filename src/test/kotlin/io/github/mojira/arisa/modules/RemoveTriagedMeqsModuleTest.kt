package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule.Comment
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class RemoveTriagedMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no priority and no triaged time" {
        val module = RemoveTriagedMeqsModule(emptyList(), "")
        val request = Request(null, null, emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveTriagedMeqsModule(emptyList(), "")
        val request = Request("Important", "triaged", emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = Comment("I like QC.") { Unit.right() }
        val request = Request("Important", "triaged", listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = Comment("MEQS_WAI I like QC.") { RuntimeException().left() }
        val request = Request("Important", "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = Comment("MEQS_WAI I like QC.") { RuntimeException().left() }
        val request = Request("Important", "triaged", listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should process tickets with Mojang Priority" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = Comment("MEQS_WAI I like QC.") { Unit.right() }
        val request = Request("Important", null, listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should process tickets with triaged time" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = Comment("MEQS_WAI I like QC.") { Unit.right() }
        val request = Request(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should replace only MEQS of a tag" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = Comment("MEQS_WAI\nI like QC.") { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC.").right() }
        val request = Request(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should not replace MEQS of tags that aren't configured" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = Comment("MEQS_WAI\nMEQS_TRIVIAL\nI like QC.") { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nMEQS_TRIVIAL\nI like QC.").right() }
        val request = Request(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should replace MEQS of all configured tags" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI", "MEQS_WONTFIX"), "Test.")
        val comment = Comment("MEQS_WAI\nMEQS_WONTFIX\nI like QC.") { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nMEQS_ARISA_REMOVED_WONTFIX Removal Reason: Test.\nI like QC.").right() }
        val request = Request(null, "triaged", listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
