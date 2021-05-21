package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RemoveTriagedMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no priority and no triaged time" {
        val module = RemoveTriagedMeqsModule(emptyList(), "")
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveTriagedMeqsModule(emptyList(), "")
        val issue = mockIssue(
            priority = "Important",
            triagedTime = "triaged"
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = mockComment(
            body = "I like QC."
        )
        val issue = mockIssue(
            priority = "Important",
            triagedTime = "triaged",
            comments = listOf(comment, comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should process tickets with Mojang Priority" {
        var editedComment = ""
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Lorem Ipsum.")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            update = { editedComment = it }
        )
        val issue = mockIssue(
            priority = "Important",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "Arisa removed prefix 'MEQS' from '_WAI'; removal reason: Lorem Ipsum. I like QC."
    }

    "should process tickets with triaged time" {
        var editedComment = ""
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Lorem Ipsum.")
        val comment = mockComment(
            body = "MEQS_WAI I like QC.",
            update = { editedComment = it }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "Arisa removed prefix 'MEQS' from '_WAI'; removal reason: Lorem Ipsum. I like QC."
    }

    "should replace only MEQS of a tag" {
        var editedComment = ""
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nI like QC.",
            update = { editedComment = it }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "Arisa removed prefix 'MEQS' from '_WAI'; removal reason: Test.\nI like QC."
    }

    "should not replace MEQS of tags that aren't configured" {
        var editedComment = ""
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nMEQS_TRIVIAL\nI like QC.",
            update = { editedComment = it }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "Arisa removed prefix 'MEQS' from '_WAI'; removal reason: Test.\nMEQS_TRIVIAL\nI like QC."
    }

    "should replace MEQS of all configured tags" {
        var editedComment = ""
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI", "MEQS_WONTFIX"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nMEQS_WONTFIX\nI like QC.",
            update = { editedComment = it }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        editedComment shouldBe "Arisa removed prefix 'MEQS' from '_WAI'; removal reason: Test.\nArisa removed prefix 'MEQS' from '_WONTFIX'; removal reason: Test.\nI like QC."
    }
})
