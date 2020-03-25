package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Visibility

class RemoveNonStaffMeqsModuleTest : StringSpec({

    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val request = RemoveNonStaffMeqsModuleRequest(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val comment = mockComment("I like QC.")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for a staff restricted comment" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val comment = mockComment("MEQS_WAI I like QC.", true)
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if MEQS is not part of a tag" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val comment = mockComment("My server has 1 MEQS of RAM and it's crashing. Also I don't know how to spell MEGS")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveNonStaffMeqsModule { _, _ -> RuntimeException().left() }
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveNonStaffMeqsModule { _, _ -> RuntimeException().left() }
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should update comment when there is an unrestricted MEQS comment" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should only remove MEQS of the comment" {
        val module = RemoveNonStaffMeqsModule { _, body -> body.shouldBe("_WAI I like QC.").right() }
        val comment = mockComment("MEQS_WAI I like QC.")
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun mockComment(body: String, isStaff: Boolean = false): Comment {
    val comment = mockk<Comment>()
    val visibility = mockk<Visibility>()
    every { visibility.value } returns if (isStaff) "staff" else null
    every { comment.body } returns body
    every { comment.visibility } returns visibility
    return comment
}
