package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.IssueSearcher
import io.github.mojira.arisa.domain.Restriction
import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import io.github.mojira.arisa.log
import net.rcarz.jiraclient.Attachment
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Issue
import java.util.concurrent.TimeUnit

/**
 * How many tickets can have the user activity removed.
 * This is a safety guard in case the command gets invoked on a very active user.
 */
private const val REMOVABLE_ACTIVITY_CAP = 200

/**
 * After how many actions the bot should pause for a second
 * (in order to not send too many requests too quickly)
 */
private const val REMOVE_USER_SLEEP_INTERVAL = 10

@Suppress("LongParameterList")
class RemoveContentCommand(
    private val issueSearcher: IssueSearcher,
    private val getIssue: (String) -> Either<Throwable, Pair<String, Issue>>,
    private val execute: (Runnable) -> Unit,

    // All of the parameters below are for easy testing.
    // They should be removed with a future refactor.

    private val getCommentsFromIssue: (String, Issue) -> List<Pair<String, Comment>> = { _, issue ->
        issue.comments.mapNotNull { it.id to it }
    },
    private val getVisibilityValueOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.visibility?.value ?: ""
    },
    private val getAuthorOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.author?.name ?: ""
    },
    private val getBodyOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.body ?: ""
    },
    private val updateComment: (Pair<String, Comment>, content: String) -> Unit = { (_, comment), content ->
        comment.update(
            content,
            "group",
            "staff"
        )
    },
    private val getAttachmentsFromIssue: (String, Issue) -> List<Pair<String, Attachment>> = { _, issue ->
        issue.attachments.mapNotNull { it.id to it }
    },
    private val getAuthorNameFromAttachment: (Pair<String, Attachment>) -> String? = { (_, attachment) ->
        attachment.author?.name
    },
    private val removeAttachment: (Pair<String, Attachment>, Issue) -> Unit = { (id, _), issue ->
        issue.removeAttachment(id)
    }
) {
    operator fun invoke(
        issue: io.github.mojira.arisa.domain.Issue,
        userName: String
    ): Int {
        val issueFunctionInner = escapeIssueFunction(userName) { "by $it" }

        val jql = """project != TRASH
            | AND issueFunction IN commented($issueFunctionInner)
            | OR issueFunction IN fileAttached($issueFunctionInner)"""
            .trimMargin().replace("[\n\r]", "")

        val ticketIds = when (val either = issueSearcher.searchIssues(jql, REMOVABLE_ACTIVITY_CAP)) {
            is Either.Left -> throw CommandExceptions.CANNOT_QUERY_USER_ACTIVITY.create(userName)
            is Either.Right -> either.b
        }

        execute {
            val result = removeActivity(ticketIds, userName)

            issue.addRawComment(
                "Removed ${result.removedComments} comments " +
                        "and ${result.removedAttachments} attachments from user \"$userName\".",
                Restriction.STAFF
            )
        }

        return 1
    }

    private data class RemoveActivityResult(var removedComments: Int, var removedAttachments: Int)

    private fun removeActivity(ticketIds: List<String>, userName: String): RemoveActivityResult {
        val result = RemoveActivityResult(0, 0)

        ticketIds
            .mapNotNull {
                val either = getIssue(it)
                if (either.isLeft()) {
                    null
                } else {
                    (either as Either.Right).b
                }
            }
            .forEach { (key, issue) ->
                log.debug("Removing comments and attachments from ticket $key")

                result.removedComments += getCommentsFromIssue(key, issue)
                    .filter { getVisibilityValueOfComment(it) != "staff" }
                    .filter { getAuthorOfComment(it) == userName }
                    .onEachIndexed { index, it ->
                        updateComment(
                            it,
                            getBodyOfComment(it).plus(
                                "\n\n~Removed by Arisa - Delete user \"$userName\"~"
                            )
                        )
                        if (index % REMOVE_USER_SLEEP_INTERVAL == 0) {
                            TimeUnit.SECONDS.sleep(1)
                        }
                    }
                    .count()

                result.removedAttachments += getAttachmentsFromIssue(key, issue)
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
