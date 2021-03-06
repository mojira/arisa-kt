package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.MAX_RESULTS
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.service.IssueService
import net.rcarz.jiraclient.JiraClient

class JiraIssueService(val jiraClient: JiraClient, val config: Config, val mapFromJira: MapFromJira) : IssueService {
    val issueCache = IssueCache(jiraClient) { mapFromJira.toDomain(it) }
    val jiraIssueCache = IssueCache(jiraClient) { it }

    override fun getIssue(key: String) = issueCache[key]

    override fun saveIssue(issue: Issue) {
        issueCache[issue.key] = issue
    }

    override fun exportIssue(issue: Issue) {
        val jiraIssue = jiraIssueCache[issue.key]
        val mapToJira = jiraIssue.mapToJira(config)
        with(issue) {
            if (securityLevel != null && securityLevel != originalIssue?.securityLevel) {
                mapToJira.updateSecurityLevel(securityLevel!!)
            }
            if (resolution != null && resolution != originalIssue?.resolution) {
                mapToJira.resolve(resolution!!)
            }
            if (chk != null && chk != originalIssue?.chk) {
                mapToJira.updateChk()
            }
            if (linked != null && linked != originalIssue?.linked) {
                mapToJira.updateLinked(linked!!)
            }
            if (addedComments.isNotEmpty()) {
                mapToJira.addComments(addedComments)
            }
            if (editedComments.isNotEmpty()) {
                mapToJira.editComments(editedComments)
            }
        }
        mapToJira.execute()
    }

    override fun searchIssues(query: String, startAt: Int): List<Issue> {
        val issues = jiraClient.searchIssues(query, "*all", "changelog", MAX_RESULTS, startAt).issues
        addToCache(issues)
        return issues.map { it.key }.map { getIssue(it) }
    }

    override fun cleanup() {
        issueCache.clear()
        jiraIssueCache.clear()
    }

    private fun addToCache(issues: List<JiraIssue>) {
        jiraIssueCache.save(issues.map { it.key to it }.toMap())
        issueCache.save(issues.map {
            val issue = mapFromJira.toDomain(it)
            it.key to issue.copy(originalIssue = issue)
        }.toMap())
    }
}