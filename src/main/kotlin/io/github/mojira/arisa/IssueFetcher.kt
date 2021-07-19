package io.github.mojira.arisa

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.jira.Mapper

/**
 * Fetches issues from Jira and maps them to domain objects.
 */
interface IssueFetcher {
    /**
     * Fetches all issues matching the JQL query string.
     *
     * @throws Exception when fetching issues fails
     */
    fun fetchAllIssues(jql: String): List<Issue>

    companion object {
        fun createFetcher(
            connectionService: JiraConnectionService,
            mapper: Mapper,
            resultsPerFetch: Int = 100
        ): IssueFetcher {
            return object : IssueFetcher {
                override fun fetchAllIssues(jql: String): List<Issue> {
                    val issues = connectionService.getCurrentJiraClient().searchIssues(
                        jql,
                        "*all",
                        "changelog",
                        resultsPerFetch,
                        0
                    )
                        .iterator()
                        .asSequence()
                        .map(mapper::toDomain)

                    return issues.toList()
                }
            }
        }
    }
}
