package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.log
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

// TODO: We use two different "Issue" classes here, which is very ugly
class RemoveUserCommand(
    val searchIssues: (String, Int) -> Either<Throwable, List<String>>,
    val getIssue: (String) -> Either<Throwable, Issue>,
    val execute: (Runnable) -> Unit
) {
    operator fun invoke(
        issue: io.github.mojira.arisa.domain.Issue,
        userName: String
    ): Int {
        val escapedUserName = userName.replace("'", "\\'")

        val jql = """project != TRASH
            | AND issueFunction IN commented("by '$escapedUserName'")
            | OR issueFunction IN fileAttached("by '$escapedUserName'")"""
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
            .forEach { issue ->
                log.debug("Removing comments and attachments from ticket ${issue.key}")

                result.removedComments += issue.comments
                    .filter { it.visibility?.value != "staff" }
                    .filter { it.author.name == userName }
                    .onEachIndexed { index, it ->
                        it.update(
                            it.body?.plus(
                                "\n\n~Removed by Arisa - Delete user \"$userName\"~"
                            ),
                            "group",
                            "staff"
                        )
                        if (index % REMOVE_USER_SLEEP_INTERVAL == 0) {
                            TimeUnit.SECONDS.sleep(1)
                        }
                    }
                    .count()

                result.removedAttachments += issue.attachments
                    .filter { it.author?.name == userName }
                    .onEachIndexed { index, it ->
                        issue.removeAttachment(it.id)
                        if (index % REMOVE_USER_SLEEP_INTERVAL == 0) {
                            TimeUnit.SECONDS.sleep(1)
                        }
                    }
                    .count()
            }

        return result
    }
}
