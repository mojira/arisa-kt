package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
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

    "should return OperationNotNeededModuleResponse for a global-moderators restricted comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "group",
            visibilityValue = "global-moderators"
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

    "should update comment when there is an unrestricted MEQS comment" {
        var editedComment = ""
        val module = RemoveNonStaffMeqsModule("Lorem Ipsum.")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            restrict = { editedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "MEQS_ARISA_REMOVED_WAI Removal Reason: Lorem Ipsum. I like QC."
    }

    "should update comment when there is a MEQS comment restricted to a group other than staff" {
        var editedComment = ""
        val module = RemoveNonStaffMeqsModule("Lorem Ipsum.")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "group",
            visibilityValue = "users",
            restrict = { editedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "MEQS_ARISA_REMOVED_WAI Removal Reason: Lorem Ipsum. I like QC."
    }

    "should update comment when there is a MEQS comment restricted to something that is not a group" {
        var editedComment = ""
        val module = RemoveNonStaffMeqsModule("Lorem Ipsum.")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            visibilityType = "user",
            visibilityValue = "staff",
            restrict = { editedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "MEQS_ARISA_REMOVED_WAI Removal Reason: Lorem Ipsum. I like QC."
    }

    "should only remove MEQS of the comment" {
        var editedComment = ""
        val module = RemoveNonStaffMeqsModule("Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nI like QC.",
            restrict = { editedComment = it }
        )
        val issue = mockIssue(
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC."
    }
})
