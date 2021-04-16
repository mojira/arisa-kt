package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.domain.service.IssueService
import io.github.mojira.arisa.domain.service.UserService
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.sf.json.JSONObject
import java.text.SimpleDateFormat

private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")

class MapFromJira(val config: Config, val userService: UserService, val issueService: IssueService) {

    fun toDomain(issue: JiraIssue) = Issue(
        issue.key,
        issue.summary,
        issue.status.name,
        issue.description,
        issue.getFieldAsString("environment"),
        issue.security?.id,
        issue.reporter?.toDomain(),
        issue.resolution?.name,
        issue.createdDate.toInstant(),
        issue.updatedDate.toInstant(),
        issue.resolutionDate?.toInstant(),
        issue.getFieldAsString(config[Arisa.CustomFields.chkField]),
        issue.getCustomField(config[Arisa.CustomFields.confirmationField]),
        issue.getFieldAsDouble(config[Arisa.CustomFields.linked]),
        issue.getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
        issue.getFieldAsString(config[Arisa.CustomFields.triagedTimeField]),
        issue.project.toDomain(),
        issue.getFieldAsString(config[Arisa.CustomFields.platformField]),
        issue.versions.map { it.toDomain() },
        issue.fixVersions.map { it.toDomain() },
        issue.attachments.map { it.toDomain() },
        issue.comments.map { it.toDomain() },
        issue.issueLinks.map { it.toDomain() },
        issue.changeLog.entries.flatMap { entry -> entry.items.map { it.toDomain(entry) } },
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
        id,
        type.name,
        outwardIssue != null,
        (outwardIssue ?: inwardIssue).toLinkedIssue()
    )

    private fun JiraIssue.toLinkedIssue() = LinkedIssue(
        key,
        status.name
    ) { issueService.getIssue(key) }

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