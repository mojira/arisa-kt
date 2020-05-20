package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class RemoveNonStaffMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveNonStaffMeqsModule("")
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "I like QC."
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for a staff restricted comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if MEQS is not part of a tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "My server has 1 MEQS of RAM and it's crashing. Also I don't know how to spell MEGS"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            restrict = { RuntimeException().left() },
            update = { RuntimeException().left() }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC .",
            restrict = { RuntimeException().left() },
            update = { RuntimeException().left() }
        )
        val issue = mockIssue(
            comments = listOf(comment, comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should update comment when there is an unrestricted MEQS comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC."
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to a group other than staff" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "group",
            visibilityValue = "users"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to something that is not a group" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "user",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should only remove MEQS of the comment" {
        val module = RemoveNonStaffMeqsModule("Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nI like QC.",
            restrict = { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC."); Unit.right() }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }
})
