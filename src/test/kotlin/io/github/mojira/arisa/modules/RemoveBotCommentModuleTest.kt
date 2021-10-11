package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)
private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)

class RemoveBotCommentModuleTest : StringSpec({
    val module = RemoveBotCommentModule(
        botUserName = "userName",
        removalTag = "ARISA_DELETE"
    )

    "should return OperationNotNeededModuleResponse when there is no comment" {
        val issue = mockIssue()

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment was edited before last run" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "ARISA_DELETE",
                    created = TWO_SECONDS_AGO,
                    author = mockUser(
                        name = "userName"
                    ),
                    visibilityType = "group",
                    visibilityValue = "staff"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment does not contain removal tag" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "DELETE",
                    author = mockUser(
                        name = "userName"
                    ),
                    visibilityType = "group",
                    visibilityValue = "staff"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment was not created by bot" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "ARISA_DELETE",
                    author = mockUser(
                        name = "anotherUserName"
                    ),
                    visibilityType = "group",
                    visibilityValue = "staff"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is not restricted" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "ARISA_DELETE",
                    author = mockUser(
                        name = "userName"
                    )
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove comment if removal tag is present and the author of the comment is the bot" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "ARISA_DELETE",
                    author = mockUser(
                        name = "userName"
                    ),
                    visibilityType = "group",
                    visibilityValue = "staff"
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }
})
