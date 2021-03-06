package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.new.Issue
import io.github.mojira.arisa.domain.service.IssueService
import net.rcarz.jiraclient.JiraClient

class JiraIssueService(val jiraClient: JiraClient, val config: Config, val mapFromJira: MapFromJira) : IssueService {
    val issueCache = IssueCache(jiraClient) { mapFromJira.toDomain(it) }
    val jiraIssueCache = IssueCache(jiraClient) { it }

    override fun getIssue(key: String): Issue {
        val issue = issueCache[key]
        return issue.copy(originalIssue = issue)
    }

    override fun saveIssue(issue: Issue) {
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
        issueCache[issue.key] = issue.copy(originalIssue = null)
    }

    fun addToCache(issues: List<JiraIssue>) {
        jiraIssueCache.save(issues.map { it.key to it }.toMap())
        issueCache.save(issues.map { it.key to mapFromJira.toDomain(it) }.toMap())
    }

    fun emptyCache() {
        issueCache.clear()
        jiraIssueCache.clear()
    }
}