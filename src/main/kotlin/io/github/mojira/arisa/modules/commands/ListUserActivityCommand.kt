package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.log

/**
 * How many tickets should be listed at max.
 * This is a safety guard in case the command gets invoked on a very active user.
 * We don't want the comment to become too long.
 */
const val ACTIVITY_LIST_CAP = 50

class ListUserActivityCommand(
    val searchIssues: (String, Int) -> Either<Throwable, List<String>>
) : Command1<String> {
    override fun invoke(issue: Issue, userName: String): Int {
        val escapedUserName = userName.replace("'", "\\'")

        val jql = """issueFunction IN commented("by '$escapedUserName'")
            | OR issueFunction IN fileAttached("by '$escapedUserName'")"""
            .trimMargin().replace("[\n\r]", "")

        val tickets = when (val either = searchIssues(jql, ACTIVITY_LIST_CAP)) {
            is Either.Left -> {
                log.error("ListUserActivityCommand: Error trying to query user activity", either.a)
                issue.addRawRestrictedComment(
                    "Could not query activity of user \"$userName\":\n* {{${either.a.message}}}",
                    "staff"
                )
                return 1
            }
            is Either.Right -> either.b
        }

        if (tickets.isNotEmpty()) {
            issue.addRawRestrictedComment(
                "User \"$userName\" left comments on the following tickets:\n* ${tickets.joinToString("\n* ")}",
                "staff"
            )
        } else {
            issue.addRawRestrictedComment(
                """No unrestricted comments from user "$userName" were found.""",
                "staff"
            )
        }

        return 1
    }
}
