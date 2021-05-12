package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.service.IssueService
import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import io.github.mojira.arisa.log
import java.util.concurrent.TimeUnit

/**
 * How many tickets can have the user activity removed.
 * This is a safety guard in case the command gets invoked on a very active user.
 */
const val REMOVABLE_ACTIVITY_CAP = 200

/**
 * After how many actions the bot should pause for a second
 * (in order to not send too many requests too quickly)
 */
const val REMOVE_USER_SLEEP_INTERVAL = 10

@Suppress("LongParameterList")
class RemoveContentCommand(
    val issueService: IssueService,
    val execute: (Runnable) -> Unit,

    // All of the parameters below are for easy testing.
    // They should be removed with a future refactor.

    val getCommentsFromIssue: (String, Issue) -> List<Pair<String, Comment>> = { _, issue ->
        issue.comments.filter { it.id != null }.map { it.id!! to it }
    },
    val getVisibilityValueOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.visibilityValue ?: ""
    },
    val getAuthorOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.author?.name ?: ""
    },
    val getBodyOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.body ?: ""
    },
    val updateComment: (Pair<String, Comment>, content: String, issue: Issue) -> Unit = { (_, comment), content, issue ->
        issue.editedComments.add(comment.copy(body = content, visibilityType = "group", visibilityValue = "staff"))
    },
    val getAttachmentsFromIssue: (String, Issue) -> List<Pair<String, Attachment>> = { _, issue ->
        issue.attachments.filter { it.id != null }.map { it.id!! to it }
    },
    val getAuthorNameFromAttachment: (Pair<String, Attachment>) -> String? = { (_, attachment) ->
        attachment.uploader?.name
    },
    val removeAttachment: (Pair<String, Attachment>, Issue) -> Unit = { (_, attachment), issue ->
        issue.removedAttachments.add(attachment)
    }
) {
    operator fun invoke(
        issue: Issue,
        userName: String
    ): Int {
        val issueFunctionInner = escapeIssueFunction(userName) { "by $it" }

        val jql = """project != TRASH
            | AND issueFunction IN commented($issueFunctionInner)
            | OR issueFunction IN fileAttached($issueFunctionInner)"""
            .trimMargin().replace("[\n\r]", "")

        val ticketIds = issueService.searchIssues(jql)
        if (ticketIds.isEmpty()) {
            throw CommandExceptions.CANNOT_QUERY_USER_ACTIVITY.create(userName)
        }

        execute {
            val result = removeActivity(ticketIds, userName)

            issue.addRawComment(
                "Removed ${result.removedComments} comments " +
                        "and ${result.removedAttachments} attachments from user \"$userName\".",
                "group", "staff"
            )
        }

        return 1
    }

    private data class RemoveActivityResult(var removedComments: Int, var removedAttachments: Int)

    private fun removeActivity(ticketIds: List<Issue>, userName: String): RemoveActivityResult {
        val result = RemoveActivityResult(0, 0)

        ticketIds
            .forEach { issue ->
                log.debug("Removing comments and attachments from ticket ${issue.key}")

                result.removedComments += getCommentsFromIssue(issue.key, issue)
                    .filter { getVisibilityValueOfComment(it) != "staff" }
                    .filter { getAuthorOfComment(it) == userName }
                    .onEachIndexed { index, it ->
                        updateComment(
                            it,
                            getBodyOfComment(it).plus(
                                "\n\n~Removed by Arisa - Delete user \"$userName\"~"
                            ),
                            issue
                        )
                        if (index % REMOVE_USER_SLEEP_INTERVAL == 0) {
                            TimeUnit.SECONDS.sleep(1)
                        }
                    }
                    .count()

                result.removedAttachments += getAttachmentsFromIssue(issue.key, issue)
                    .filter { getAuthorNameFromAttachment(it) == userName }
                    .onEachIndexed { index, it ->
                        removeAttachment(it, issue)
                        if (index % REMOVE_USER_SLEEP_INTERVAL == 0) {
                            TimeUnit.SECONDS.sleep(1)
                        }
                    }
                    .count()
            }

        return result
    }
}
