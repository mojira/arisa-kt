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
        val visibility = mockVisibility("group", "staff")
        val comment = mockComment("MEQS_WAI I like QC.", visibility)
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

    "should update comment when there is a MEQS comment restricted to a group other than staff" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val visibility = mockVisibility("group", "users")
        val comment = mockComment("MEQS_WAI I like QC.", visibility)
        val request = RemoveNonStaffMeqsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to something that is not a group" {
        val module = RemoveNonStaffMeqsModule { _, _ -> Unit.right() }
        val visibility = mockVisibility("user", "staff")
        val comment = mockComment("MEQS_WAI I like QC.", visibility)
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

private fun mockComment(body: String, visibility: Visibility? = null): Comment {
    val comment = mockk<Comment>()
    every { comment.body } returns body
    every { comment.visibility } returns visibility
    return comment
}

private fun mockVisibility(type: String, value: String): Visibility {
    val visibility = mockk<Visibility>()
    every { visibility.type } returns type
    every { visibility.value } returns value
    return visibility
}
