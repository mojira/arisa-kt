package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule.Comment
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class RemoveNonStaffMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveNonStaffMeqsModule("")
        val request = Request(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("I like QC.", null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for a staff restricted comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", "group", "staff") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if MEQS is not part of a tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("My server has 1 MEQS of RAM and it's crashing. Also I don't know how to spell MEGS", null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", null, null) { RuntimeException().left() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", null, null) { RuntimeException().left() }
        val request = Request(listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should update comment when there is an unrestricted MEQS comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to a group other than staff" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", "group", "users") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to something that is not a group" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment("MEQS_WAI I like QC.", "user", "staff") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should only remove MEQS of the comment" {
        val module = RemoveNonStaffMeqsModule("Test.")
        val comment = Comment("MEQS_WAI\nI like QC.", null, null) { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC.").right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
