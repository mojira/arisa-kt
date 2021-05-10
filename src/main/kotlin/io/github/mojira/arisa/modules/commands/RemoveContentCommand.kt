package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.log
import net.rcarz.jiraclient.Attachment
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Issue
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
    val searchIssues: (String, Int) -> Either<Throwable, List<String>>,
    val getIssue: (String) -> Either<Throwable, Pair<String, Issue>>,
    val execute: (Runnable) -> Unit,

    // All of the parameters below are for easy testing.
    // They should be removed with a future refactor.

    val getCommentsFromIssue: (String, Issue) -> List<Pair<String, Comment>> = { _, issue ->
        issue.comments.mapNotNull { it.id to it }
    },
    val getVisibilityValueOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.visibility?.value ?: ""
    },
    val getAuthorOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.author?.name ?: ""
    },
    val getBodyOfComment: (Pair<String, Comment>) -> String = { (_, comment) ->
        comment.body ?: ""
    },
    val updateComment: (Pair<String, Comment>, content: String) -> Unit = { (_, comment), content ->
        comment.update(
            content,
            "group",
            "staff"
        )
    },
    val getAttachmentsFromIssue: (String, Issue) -> List<Pair<String, Attachment>> = { _, issue ->
        issue.attachments.mapNotNull { it.id to it }
    },
    val getAuthorNameFromAttachment: (Pair<String, Attachment>) -> String? = { (_, attachment) ->
        attachment.author?.name
    },
    val removeAttachment: (Pair<String, Attachment>, Issue) -> Unit = { (id, _), issue ->
        issue.removeAttachment(id)
    }
) {
    operator fun invoke(
        issue: io.github.mojira.arisa.domain.Issue,
        userName: String
    ): Int {
        val escapedUserName = if (userName.contains('\'')) "\"$userName\"" else "'$userName'"

        val jql = """project != TRASH
            | AND issueFunction IN commented("by $escapedUserName")
            | OR issueFunction IN fileAttached("by $escapedUserName")"""
            .trimMargin().replace("[\n\r]", "")

        val ticketIds = when (val either = searchIssues(jql, REMOVABLE_ACTIVITY_CAP)) {
            is Either.Left -> throw CommandExceptions.CANNOT_QUERY_USER_ACTIVITY.create(userName)
            is Either.Right -> either.b
        }

        execute {
            val result = removeActivity(ticketIds, userName)

            issue.addRawRestrictedComment(
                "Removed ${result.removedComments} comments " +
                        "and ${result.removedAttachments} attachments from user \"$userName\".",
                "staff"
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
