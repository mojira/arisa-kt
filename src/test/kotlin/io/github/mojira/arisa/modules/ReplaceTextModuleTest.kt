package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)
private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)

class ReplaceTextModuleTest : StringSpec({
    val module = ReplaceTextModule()

    "should return OperationNotNeededModuleResponse when there is no description nor comment" {
        val issue = mockIssue()

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment is created before last run" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "https://bugs.mojang.com/browse/MC-1",
                    created = TWO_SECONDS_AGO
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment doesn't need replace" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "MC-1"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not a ticket ID" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "[A similar issue with this was fixed previously|https://bugs.mojang.com/browse/MC-4]"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /browse link" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "[MC-5|https://bugs.mojang.com/browse/MC-4]"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /projects link" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "[MC-5|https://bugs.mojang.com/projects/MC/issues/MC-4]"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query paramerters in comments" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links with query paramerters in comments" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links which ends in a slash with query paramerters in comments" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MCPE-38374/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links which ends in a slash with query paramerters in comments" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query in description" {
        val issue = mockIssue(
            description = "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should replace /browse links in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MC-4",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace /projects links in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace /browse links with two-digit ticket key in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MC-44",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-44"
    }

    "should replace /projects links with two-digit ticket key in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-44"
    }

    "should replace /browse links which ends with a slash in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MC-4/",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace /projects links which ends with a slash in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace /browse links with two-digit ticket key which ends with a slash in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/browse/MC-44/",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-44"
    }

    "should replace /projects links with two-digit ticket key which ends with a slash in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44/",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-44"
    }

    "should replace titled /browse links in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates [MC-4|https://bugs.mojang.com/browse/MC-4]",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace titled /projects links in comments" {
        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "Duplicates [MC-4|https://bugs.mojang.com/projects/MC/issues/MC-4]",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment shouldBe "Duplicates MC-4"
    }

    "should replace multiple comments" {
        var hasUpdatedComment0: String? = null
        var hasUpdatedComment1: String? = null
        var hasUpdatedComment2: String? = null

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]",
                    update = {
                        hasUpdatedComment0 = it
                        Unit.right()
                    }),
                mockComment(
                    body = "Check https://bugs.mojang.com/browse/MC-106013 too",
                    updated = RIGHT_NOW.plusSeconds(1),
                    update = {
                        hasUpdatedComment1 = it
                        Unit.right()
                    }
                ),
                mockComment(
                    body = "Oops, sorry!",
                    updated = RIGHT_NOW.plusSeconds(2),
                    update = {
                        hasUpdatedComment2 = it
                        Unit.right()
                    }
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment0 shouldBe "This is a duplicate of MC-4"
        hasUpdatedComment1 shouldBe "Check MC-106013 too"
        hasUpdatedComment2 shouldBe null
    }

    "should replace /browse links in description" {
        var hasUpdatedDescription: String? = null

        val issue = mockIssue(
            description = "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "A mod in MC-4 said that I have to report it in a new ticket."
    }

    "should replace both description and comments" {
        var replacedDescription: String? = null

        var hasUpdatedComment: String? = null

        val issue = mockIssue(
            description = "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            comments = listOf(
                mockComment(
                    body = "This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            ),
            updateDescription = {
                replacedDescription = it
                Unit.right()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        replacedDescription shouldBe "A mod in MC-4 said that I have to report it in a new ticket."
        hasUpdatedComment shouldBe "This is a duplicate of MC-4"
    }
})
