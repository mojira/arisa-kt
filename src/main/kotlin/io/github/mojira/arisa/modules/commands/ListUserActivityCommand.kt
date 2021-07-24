package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.jira.sanitizeCommentArg

/**
 * How many tickets should be listed at max.
 * This is a safety guard in case the command gets invoked on a very active user.
 * We don't want the comment to become too long.
 */
const val ACTIVITY_LIST_CAP = 50

class ListUserActivityCommand(
    val searchIssues: (String, Int) -> Either<Throwable, List<String>>
) {
    operator fun invoke(issue: Issue, userName: String): Int {
        val escapedUserName = userName.replace("'", "\\'")

        val jql = """issueFunction IN commented("by '$escapedUserName'")
            | OR issueFunction IN fileAttached("by '$escapedUserName'")"""
            .trimMargin().replace("[\n\r]", "")

        val sanitizedUserName = sanitizeCommentArg(userName)

        val tickets = when (val either = searchIssues(jql, ACTIVITY_LIST_CAP)) {
            is Either.Left -> throw CommandExceptions.CANNOT_QUERY_USER_ACTIVITY.create(sanitizedUserName)
            is Either.Right -> either.b
        }

        if (tickets.isNotEmpty()) {
            issue.addRawRestrictedComment(
                "User \"$sanitizedUserName\" left comments on the following tickets:" +
                    "\n* ${tickets.joinToString("\n* ")}",
                "staff"
            )
        } else {
            issue.addRawRestrictedComment(
                """No unrestricted comments from user "$sanitizedUserName" were found.""",
                "staff"
            )
        }

        return 1
    }
}
