package io.github.mojira.arisa

import arrow.core.Either
import io.github.mojira.arisa.infrastructure.jira.getIssuesFromJql

/**
 * Searches for issues matching a JQL query.
 */
interface IssueSearcher {
    /**
     * Searches for issues matching the JQL query and returns their issue keys, e.g. `"MC-1"`.
     * At most [maxResults] are returned.
     */
    fun searchIssues(jql: String, maxResults: Int): Either<Throwable, List<String>>

    companion object {
        fun createSearcher(connectionService: JiraConnectionService): IssueSearcher {
            return object : IssueSearcher {
                override fun searchIssues(jql: String, maxResults: Int): Either<Throwable, List<String>> {
                    return getIssuesFromJql(connectionService.getCurrentJiraClient(), jql, maxResults)
                }
            }
        }
    }
}
