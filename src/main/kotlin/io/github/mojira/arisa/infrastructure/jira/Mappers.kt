@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
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
import io.github.mojira.arisa.infrastructure.HelperMessages
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat
import net.rcarz.jiraclient.Attachment as JiraAttachment
import net.rcarz.jiraclient.ChangeLogEntry as JiraChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem as JiraChangeLogItem
import net.rcarz.jiraclient.Comment as JiraComment
import net.rcarz.jiraclient.Issue as JiraIssue
import net.rcarz.jiraclient.IssueLink as JiraIssueLink
import net.rcarz.jiraclient.Project as JiraProject
import net.rcarz.jiraclient.User as JiraUser
import net.rcarz.jiraclient.Version as JiraVersion

fun JiraAttachment.toDomain(jiraClient: JiraClient) = Attachment(
    fileName, createdDate.toInstant(), ::deleteAttachment.partially1(jiraClient).partially1(this), this::download
)

fun JiraProject.getSecurityLevelId(config: Config) =
    config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]

fun JiraVersion.toDomain(issue: JiraIssue) = Version(
    id,
    isReleased,
    isArchived,
    releaseDate.toReleaseDateInstant(),
    ::addAffectedVersion.partially1(issue).partially1(this),
    ::removeAffectedVersion.partially1(issue).partially1(this)
)

fun JiraIssue.toDomain(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config
) = Issue(
    key,
    summary,
    status.name,
    description,
    getEnvironment(),
    security?.id,
    reporter.toDomain(),
    resolution?.name,
    createdDate.toInstant(),
    updatedDate.toInstant(),
    getCHK(config),
    getConfirmation(config),
    getLinked(config),
    getPriority(config),
    getTriagedTime(config),
    getLinked(config),
    project.toDomain(this, config),
    mapVersions(),
    mapAttachments(jiraClient),
    mapComments(jiraClient),
    mapLinks(jiraClient, messages, config),
    getChangeLogEntries(jiraClient),
    ::reopenIssue.partially1(this),
    ::resolveAs.partially1(this).partially1("Invalid"),
    ::resolveAs.partially1(this).partially1("Duplicate"),
    ::resolveAs.partially1(this).partially1("Incomplete"),
    ::updateDescription.partially1(this),
    ::updateCHK.partially1(this).partially1(config[Arisa.CustomFields.chkField]),
    ::updateConfirmation.partially1(this).partially1(config[Arisa.CustomFields.confirmationField]),
    ::updateLinked.partially1(this).partially1(config[Arisa.CustomFields.linked]),
    ::updateSecurity.partially1(this).partially1(project.getSecurityLevelId(config)),
    ::createLink.partially1(this).partially1("Duplicate"),
    ::createComment.partially1(this).partially1(
        messages.getMessageWithBotSignature(
            project.key, config[Arisa.Modules.Empty.message]
        )
    ),
    ::createComment.partially1(this).partially1(
        messages.getMessageWithBotSignature(
            project.key, config[Arisa.Modules.Crash.moddedMessage]
        )
    ),
    { key ->
        createComment(
            this, messages.getMessageWithBotSignature(
                project.key, config[Arisa.Modules.Crash.duplicateMessage], key
            )
        )
    },
    ::createComment.partially1(this).partially1(
        messages.getMessageWithBotSignature(
            project.key, config[Arisa.Modules.FutureVersion.message]
        )
    ),
    ::createComment.partially1(this).partially1(
        messages.getMessageWithBotSignature(
            project.key, config[Arisa.Modules.KeepPrivate.message]
        )
    ),
    { language ->
        // Should we move this?
        // Most likely, no ;D
        // addRestrictedComment(issue, messages.getMessageWithBotSignature(
        //     issue.project.key, config[Modules.Language.message], lang = language
        // ), "helper")
        val translatedMessage = config[Arisa.Modules.Language.messages][language]
        val defaultMessage = config[Arisa.Modules.Language.defaultMessage]
        val text =
            if (translatedMessage != null) config[Arisa.Modules.Language.messageFormat].format(
                translatedMessage,
                defaultMessage
            ) else defaultMessage

        addRestrictedComment(
            this,
            text,
            "helper"
        )
    },
    ::createComment.partially1(this).partially1(
        messages.getMessageWithBotSignature(
            project.key, config[Arisa.Modules.Piracy.message]
        )
    )
)

fun JiraProject.toDomain(
    issue: JiraIssue,
    config: Config
) = Project(
    key,
    versions.map { it.toDomain(issue) },
    getSecurityLevelId(config)
)

fun JiraComment.toDomain(
    jiraClient: JiraClient
) = Comment(
    body,
    author.toDomain(),
    { getGroups(jiraClient, author.name).fold({ null }, { it }) },
    createdDate.toInstant(),
    updatedDate.toInstant(),
    visibility?.type,
    visibility?.value,
    ::restrictCommentToGroup.partially1(this).partially1("staff"),
    ::updateCommentBody.partially1(this)
)

fun JiraUser.toDomain() = User(
    name, displayName
)

private fun getUserGroups(jiraClient: JiraClient, username: String) = getGroups(
    jiraClient,
    username
).fold({ null }, { it })

fun JiraIssue.toLinkedIssue(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config
) = LinkedIssue(
    key,
    status.name,
    ::getFullIssue.partially1(jiraClient).partially1(messages).partially1(config),
    ::createLink.partially1(this),
    ::addAffectedVersionById.partially1(this)
)

fun JiraIssueLink.toDomain(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config
) = Link(
    type.name,
    outwardIssue != null,
    (outwardIssue ?: inwardIssue).toLinkedIssue(jiraClient, messages, config),
    ::deleteLink.partially1(this)
)

fun JiraChangeLogItem.toDomain(jiraClient: JiraClient, entry: JiraChangeLogEntry) = ChangeLogItem(
    entry.created.toInstant(),
    field,
    fromString,
    toString,
    entry.author.toDomain(),
    ::getUserGroups.partially1(jiraClient).partially1(entry.author.name)
)

private fun JiraIssue.mapLinks(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config
) = issueLinks.map { it.toDomain(jiraClient, messages, config) }

private fun JiraIssue.mapComments(jiraClient: JiraClient) =
    comments.map { it.toDomain(jiraClient) }

private fun JiraIssue.mapAttachments(jiraClient: JiraClient) =
    attachments.map { it.toDomain(jiraClient) }

private fun JiraIssue.mapVersions() = versions.map { it.toDomain(this) }

fun JiraIssue.getChangeLogEntries(jiraClient: JiraClient) =
    changeLog.entries.flatMap { e ->
        e.items.map { i ->
            i.toDomain(jiraClient, e)
        }
    }

private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?

private fun JiraIssue.getCustomField(customField: String): String? =
    ((getField(customField)) as? JSONObject)?.get("value") as? String?

fun JiraIssue.getEnvironment() = getFieldAsString("environment")

fun JiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
fun JiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
fun JiraIssue.getLinked(config: Config) = getField(config[Arisa.CustomFields.linked]) as? Double?
fun JiraIssue.getPriority(config: Config) = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
fun JiraIssue.getTriagedTime(config: Config) = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
private val releaseDateFormat = SimpleDateFormat("yyyy-MM-dd")
fun String.toReleaseDateInstant() = releaseDateFormat.parse(this).toInstant()

fun JiraIssue.getFullIssue(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config
): Either<Throwable, Issue> =
    getIssue(jiraClient, key).fold(
        { it.left() },
        { it.toDomain(jiraClient, messages, config).right() }
    )
