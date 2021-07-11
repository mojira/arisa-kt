package io.github.mojira.arisa.infrastructure.newjira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.newdomain.Attachment
import io.github.mojira.arisa.newdomain.ChangeLogItem
import io.github.mojira.arisa.newdomain.Comment
import io.github.mojira.arisa.newdomain.Issue
import io.github.mojira.arisa.newdomain.Link
import io.github.mojira.arisa.newdomain.LinkedIssue
import io.github.mojira.arisa.newdomain.Project
import io.github.mojira.arisa.newdomain.User
import io.github.mojira.arisa.newdomain.Version
import net.sf.json.JSONObject
import java.text.SimpleDateFormat

private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")

@Suppress("TooManyFunctions")
class MapFromJira(
    val config: Config,
    val userService: UserService,
    val attachmentService: AttachmentService
) {

    fun JiraIssue.toDomain() = Issue(
        this.key,
        this.summary ?: "",
        this.status?.name,
        this.description ?: "",
        this.getFieldAsString("environment") ?: "",
        this.security?.id,
        this.reporter?.toDomain(),
        this.resolution?.name,
        this.createdDate.toInstant(),
        this.updatedDate?.toInstant(),
        this.resolutionDate?.toInstant(),
        this.getFieldAsString(config[Arisa.CustomFields.chkField]),
        this.getCustomField(config[Arisa.CustomFields.confirmationField]),
        this.getFieldAsDouble(config[Arisa.CustomFields.linked]),
        this.getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
        this.getFieldAsString(config[Arisa.CustomFields.triagedTimeField]),
        this.project.toDomain(),
        this.getFieldAsString(config[Arisa.CustomFields.platformField]),
        this.getFieldAsString(config[Arisa.CustomFields.dungeonsPlatformField]),
        this.versions.map { it.toDomain() },
        mutableListOf(),
        this.fixVersions.map { it.toDomain() },
        mutableListOf(),
        this.attachments.map { it.toDomain() },
        mutableListOf(),
        this.comments.map { it.toDomain() },
        mutableListOf(),
        this.issueLinks.map { it.toDomain() },
        mutableListOf(),
        this.changeLog.entries.flatMap { entry -> entry.items.map { it.toDomain(entry) } })

    private fun JiraUser.toDomain() = User(
        this.name,
        this.displayName,
        userService.getGroups(this.name),
        userService.isNewUser(this.name)
    )

    private fun JiraProject.toDomain() = Project(
        this.key,
        this.versions.map { it.toDomain() },
        config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]
    )

    private fun JiraAttachment.toDomain() = Attachment(
        this.id,
        this.fileName,
        this.createdDate.toInstant(),
        this.mimeType,
        { attachmentService.openContentStream(this.id, this.contentUrl) },
        { this.download() },
        this.author?.toDomain(),
        true
    )

    private fun JiraComment.toDomain() = Comment(
        this.body,
        this.author.toDomain(),
        this.createdDate.toInstant(),
        this.updatedDate?.toInstant(),
        this.visibility?.type,
        this.visibility?.value
    )

    private fun JiraVersion.toDomain() = Version(
        this.id,
        this.name,
        this.isReleased,
        this.isArchived,
        this.releaseDate?.let { versionDateFormat.parse(it) }?.toInstant()
    )

    private fun JiraIssueLink.toDomain() = Link(
        this.type.name,
        this.outwardIssue != null,
        (this.outwardIssue ?: this.inwardIssue).toLinkedIssue(),
        true
    )

    private fun JiraIssue.toLinkedIssue() = LinkedIssue(
        this.key,
        this.status.name
    )

    private fun JiraChangelogItem.toDomain(entry: JiraChangelogEntry) = ChangeLogItem(
        entry.created.toInstant(),
        this.field,
        this.from,
        this.fromString,
        this.to,
        this.toString,
        entry.author.toDomain()
    )

    private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?
    private fun JiraIssue.getCustomField(field: String) = ((getField(field)) as? JSONObject)?.get("value") as? String?
    private fun JiraIssue.getFieldAsDouble(field: String) = this.getField(field) as? Double?
}
