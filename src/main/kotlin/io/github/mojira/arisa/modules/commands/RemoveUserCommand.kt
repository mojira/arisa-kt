package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.log
import net.rcarz.jiraclient.Issue
import java.util.concurrent.TimeUnit

/**
 * How many tickets can have the user activity removed. This is a safety guard in case the command gets invoked on a very active user.
 */
const val REMOVABLE_ACTIVITY_CAP = 200

/**
 * After how many actions the bot should pause for a second (in order to not send too many requests too quickly)
 */
const val SLEEP_INTERVAL = 10

// TODO: We use two different "Issue" classes here, which is very ugly
class RemoveUserCommand(
    val searchIssues: (String, Int) -> Either<Throwable, List<String>>,
    val getIssue: (String) -> Either<Throwable, Issue>,
    val execute: (Runnable) -> Unit
) : Command1<String> {
    override fun invoke(
        issue: io.github.mojira.arisa.domain.Issue,
        userName: String
    ): Int {
        val escapedUserName = userName.replace("'", "\\'")

        val jql = """project != TRASH
            | AND issueFunction IN commented("by '$escapedUserName'")
            | OR issueFunction IN fileAttached("by '$escapedUserName'")"""
            .trimMargin().replace("[\n\r]", "")

        val ticketIds = when (val either = searchIssues(jql, REMOVABLE_ACTIVITY_CAP)) {
            is Either.Left -> {
                log.error("RemoveUserCommand: Error trying to query user activity", either.a)
                issue.addRawRestrictedComment(
                    "Could not query activity of user \"$userName\":\n* {{${either.a.message}}}",
                    "staff"
                )
                return 1
            }
            is Either.Right -> either.b
        }

        execute {
            var removedComments = 0
            var removedAttachments = 0

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

                    removedComments += issue.comments
                        .filter { it.visibility?.value != "staff" }
                        .filter { it.author.name == userName }
                        .onEachIndexed { index, it ->
                            it.update(it.body?.plus("\n\n~Removed by Arisa - Delete user \"$userName\"~"), "group", "staff")
                            if (index % SLEEP_INTERVAL == 0) {
                                TimeUnit.SECONDS.sleep(1)
                            }
                        }
                        .count()

                    removedAttachments += issue.attachments
                        .filter { it.author?.name == userName }
                        .onEachIndexed { index, it ->
                            issue.removeAttachment(it.id)
                            if (index % SLEEP_INTERVAL == 0) {
                                TimeUnit.SECONDS.sleep(1)
                            }
                        }
                        .count()
                }

            issue.addRawRestrictedComment(
                "Removed $removedComments comments and $removedAttachments attachments from user \"$userName\".",
                "staff"
            )
        }
        return 1
    }
}
