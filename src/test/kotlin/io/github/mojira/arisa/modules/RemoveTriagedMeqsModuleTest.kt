package io.github.mojira.arisa.modules

import arrow.core.right
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
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = mockComment(
            body = "MEQS_WAI I like QC."
        )
        val issue = mockIssue(
            priority = "Important",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should process tickets with triaged time" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "")
        val comment = mockComment(
            body = "MEQS_WAI I like QC."
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should replace only MEQS of a tag" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nI like QC.",
            update = { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC.").right() }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should not replace MEQS of tags that aren't configured" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nMEQS_TRIVIAL\nI like QC.",
            update = { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nMEQS_TRIVIAL\nI like QC.").right() }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should replace MEQS of all configured tags" {
        val module = RemoveTriagedMeqsModule(listOf("MEQS_WAI", "MEQS_WONTFIX"), "Test.")
        val comment = mockComment(
            body = "MEQS_WAI\nMEQS_WONTFIX\nI like QC.",
            update = {
                it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nMEQS_ARISA_REMOVED_WONTFIX Removal Reason: Test.\nI like QC.")
                    .right()
            }
        )
        val issue = mockIssue(
            triagedTime = "triaged",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }
})
