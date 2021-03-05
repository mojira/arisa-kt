package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.new.Attachment
import io.github.mojira.arisa.domain.new.ChangeLogItem
import io.github.mojira.arisa.domain.new.Comment
import io.github.mojira.arisa.domain.new.Issue
import io.github.mojira.arisa.domain.new.Link
import io.github.mojira.arisa.domain.new.LinkedIssue
import io.github.mojira.arisa.domain.new.Project
import io.github.mojira.arisa.domain.new.User
import io.github.mojira.arisa.domain.new.Version
import io.github.mojira.arisa.domain.service.IssueService
import io.github.mojira.arisa.domain.service.UserService
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat

typealias JiraIssue = net.rcarz.jiraclient.Issue
typealias JiraUser = net.rcarz.jiraclient.User
typealias JiraProject = net.rcarz.jiraclient.Project
typealias JiraVersion = net.rcarz.jiraclient.Version
typealias JiraAttachment = net.rcarz.jiraclient.Attachment
typealias JiraComment = net.rcarz.jiraclient.Comment
typealias JiraIssueLink = net.rcarz.jiraclient.IssueLink
typealias JiraChangelogItem = net.rcarz.jiraclient.ChangeLogItem
typealias JiraChangelogEntry = net.rcarz.jiraclient.ChangeLogEntry

private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")


class JiraIssueService(val jiraClient: JiraClient, val config: Config, val userService: UserService) : IssueService {
    val issueCache = mutableMapOf<String, Issue>()

    override fun getIssue(key: String): Issue {
        val issue = if (issueCache.containsKey(key)) {
            issueCache[key]!!
        } else {
            val temp = jiraClient.getIssue(key, "*all", "changelog").toDomain()
            issueCache[key] = temp
            temp
        }
        return issue.copy(originalIssue = issue)
    }

    override fun saveIssue(issue: Issue) {
        issueCache[issue.key] = issue.copy(originalIssue = null)
        TODO("Write changes in jira (comparing to original issue)")
    }

    fun addToCache(issues: List<JiraIssue>) {
        issueCache.putAll(issues.map { it.key to it.toDomain() }.toMap())
    }

    fun emptyCache() {
        issueCache.clear()
    }

    fun JiraIssue.toDomain() = Issue(
        key,
        summary,
        status.name,
        description,
        getFieldAsString("environment"),
        security?.id,
        reporter?.toDomain(),
        resolution?.name,
        createdDate.toInstant(),
        updatedDate.toInstant(),
        resolutionDate?.toInstant(),
        getFieldAsString(config[Arisa.CustomFields.chkField]),
        getCustomField(config[Arisa.CustomFields.confirmationField]),
        getFieldAsDouble(config[Arisa.CustomFields.linked]),
        getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
        getFieldAsString(config[Arisa.CustomFields.triagedTimeField]),
        project.toDomain(),
        getFieldAsString(config[Arisa.CustomFields.platformField]),
        versions.map { it.toDomain() },
        fixVersions.map { it.toDomain() },
        attachments.map { it.toDomain() },
        comments.map { it.toDomain() },
        issueLinks.map { it.toDomain() },
        changeLog.entries.flatMap { entry -> entry.items.map { it.toDomain(entry) } },
        null
    )

    private fun JiraUser.toDomain() = User(
        this.name,
        this.displayName,
        userService.getGroups(this.name)
    )

    private fun JiraProject.toDomain() = Project(
        key,
        versions.map { it.toDomain() },
        config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]
    )

    private fun JiraAttachment.toDomain() = Attachment(
        id,
        fileName,
        createdDate.toInstant(),
        mimeType,
        author?.toDomain(),
        { this.download() }
    )

    private fun JiraComment.toDomain() = Comment(
        id,
        body,
        author.toDomain(),
        createdDate.toInstant(),
        updatedDate.toInstant(),
        visibility?.type,
        visibility?.value
    )

    private fun JiraVersion.toDomain() = Version(
        id,
        name,
        isReleased,
        isArchived,
        releaseDate?.let { versionDateFormat.parse(it) }?.toInstant()
    )

    private fun JiraIssueLink.toDomain() = Link(
        type.name,
        outwardIssue != null,
        (outwardIssue ?: inwardIssue).toLinkedIssue()
    )

    private fun JiraIssue.toLinkedIssue() = LinkedIssue(
        key,
        status.name
    )

    private fun JiraChangelogItem.toDomain(entry: JiraChangelogEntry) = ChangeLogItem(
        entry.created.toInstant(),
        field,
        from,
        fromString,
        to,
        toString,
        entry.author.toDomain()
    )

    private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?
    private fun JiraIssue.getCustomField(field: String) = ((getField(field)) as? JSONObject)?.get("value") as? String?
    private fun JiraIssue.getFieldAsDouble(field: String) = this.getField(field) as? Double?
}


