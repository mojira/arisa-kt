package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.MAX_RESULTS
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.service.CommentCache
import io.github.mojira.arisa.domain.service.IssueService
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.rcarz.jiraclient.JiraClient

class JiraIssueService(
    val jiraClient: JiraClient,
    val config: Config,
    val commentCache: CommentCache,
    val mapToJira: MapToJira,
    val mapFromJira: MapFromJira
) : IssueService {
    val issueCache = IssueCache(jiraClient) { mapFromJira.toDomain(it) }
    val jiraIssueCache = IssueCache(jiraClient) { it }

    override fun getIssue(key: String) = issueCache[key]

    override fun saveIssue(issue: Issue) {
        issueCache[issue.key] = issue
    }

    override fun exportIssue(issue: Issue) {
        val jiraIssue = jiraIssueCache[issue.key]
        val builder = mapToJira.startMap(jiraIssue)
        with(issue) {
            if (securityLevel != null && securityLevel != originalIssue?.securityLevel) {
                builder.updateSecurityLevel(securityLevel!!)
            }
            if (resolution != null && resolution != originalIssue?.resolution) {
                builder.resolve(resolution!!)
            }
            if (chk != null && chk != originalIssue?.chk) {
                builder.updateChk()
            }
            if (linked != null && linked != originalIssue?.linked) {
                builder.updateLinked(linked!!)
            }
            if (addedComments.isNotEmpty()) {
                builder.addComments(addedComments)
            }
            if (editedComments.isNotEmpty()) {
                builder.editComments(editedComments)
            }
        }
        builder.execute()
    }

    override fun searchIssues(query: String, startAt: Int): List<Issue> {
        val issues = jiraClient.searchIssues(addDebugQuery(query), "*all", "changelog", MAX_RESULTS, startAt).issues
        addToCache(issues)
        return issues.map { it.key }.map { getIssue(it) }
    }

    override fun cleanup() {
        issueCache.clear()
        jiraIssueCache.clear()
    }

    private fun addDebugQuery(query: String): String {
        val ticketWhitelist = config[Arisa.Debug.ticketWhitelist] ?: emptyList()
        return if (ticketWhitelist.isNotEmpty()) {
            "key IN (${ticketWhitelist.joinToString(",")}) AND $query"
        } else {
            query
        }
    }

    private fun addToCache(issues: List<JiraIssue>) {
        jiraIssueCache.save(issues.map { it.key to it }.toMap())
        issueCache.save(issues.map {
            val issue = mapFromJira.toDomain(it)
            it.key to issue.copy(originalIssue = issue)
        }.toMap())
    }
}