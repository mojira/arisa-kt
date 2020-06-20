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
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.infrastructure.HelperMessages
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
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

fun JiraAttachment.toDomain(jiraClient: JiraClient, issue: JiraIssue, cache: IssueUpdateContextCache) = Attachment(
    fileName,
    createdDate.toInstant(),
    ::deleteAttachment.partially1(issue.getUpdateContext(jiraClient, cache)).partially1(this),
    this::download
)

fun JiraProject.getSecurityLevelId(config: Config) =
    config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]

fun JiraVersion.toDomain(jiraClient: JiraClient, issue: JiraIssue, cache: IssueUpdateContextCache) = Version(
    id,
    name,
    isReleased,
    isArchived,
    releaseDate?.toVersionReleaseInstant(),
    ::addAffectedVersion.partially1(issue.getUpdateContext(jiraClient, cache)).partially1(this),
    ::removeAffectedVersion.partially1(issue.getUpdateContext(jiraClient, cache)).partially1(this)
)

fun JiraIssue.getUpdateContext(jiraClient: JiraClient, cache: IssueUpdateContextCache): Lazy<IssueUpdateContext> =
    lazy {
        cache.get(key) ?: IssueUpdateContext(
            jiraClient,
            this,
            update(),
            transition(),
            transition()
        ).also { cache.add(key, it) }
    }

@Suppress("LongMethod")
fun JiraIssue.toDomain(
    jiraClient: JiraClient,
    project: JiraProject,
    messages: HelperMessages,
    config: Config,
    cache: IssueUpdateContextCache
): Issue {
    val context = getUpdateContext(jiraClient, cache)
    return Issue(
        key,
        summary,
        status.name,
        description,
        getEnvironment(),
        security?.id,
        reporter.toDomain(jiraClient),
        resolution?.name,
        createdDate.toInstant(),
        updatedDate.toInstant(),
        resolutionDate?.toInstant(),
        getCHK(config),
        getConfirmation(config),
        getLinked(config),
        getPriority(config),
        getTriagedTime(config),
        project.toDomain(jiraClient, this, config, cache),
        mapVersions(jiraClient, cache),
        mapAttachments(jiraClient, cache),
        mapComments(jiraClient, cache),
        mapLinks(jiraClient, messages, config, cache),
        getChangeLogEntries(jiraClient),
        ::reopen.partially1(context),
        ::resolveAs.partially1(context).partially1("Awaiting Response"),
        ::resolveAs.partially1(context).partially1("Invalid"),
        ::resolveAs.partially1(context).partially1("Duplicate"),
        ::resolveAs.partially1(context).partially1("Incomplete"),
        ::updateDescription.partially1(context),
        ::updateCHK.partially1(context).partially1(config[Arisa.CustomFields.chkField]),
        ::updateConfirmation.partially1(context).partially1(config[Arisa.CustomFields.confirmationField]),
        ::updateLinked.partially1(context).partially1(config[Arisa.CustomFields.linked]),
        ::updateSecurity.partially1(context).partially1(project.getSecurityLevelId(config)),
        ::addAffectedVersionById.partially1(context),
        ::createLink.partially1(context),
        { (messageKey, variable, language) ->
            createComment(
                context,
                messages.getMessageWithBotSignature(
                    project.key, messageKey, variable, language
                )
            )
        },
        { (messageKey, variable, language) ->
            addRestrictedComment(
                context,
                messages.getMessageWithBotSignature(
                    project.key, messageKey, variable, language
                ),
                "helper"
            )
        },
        { language ->
            createComment(
                context, messages.getMessageWithBotSignature(
                    project.key, config[Arisa.Modules.Language.message], lang = language
                )
            )
        },
        ::addRestrictedComment.partially1(context),
        ::markAsFixedWithSpecificVersion.partially1(context)
    )
}

fun JiraProject.toDomain(
    jiraClient: JiraClient,
    issue: JiraIssue,
    config: Config,
    cache: IssueUpdateContextCache
) = Project(
    key,
    versions.map { it.toDomain(jiraClient, issue, cache) },
    getSecurityLevelId(config)
)

fun JiraComment.toDomain(
    jiraClient: JiraClient,
    issue: JiraIssue,
    cache: IssueUpdateContextCache
): Comment {
    val context = issue.getUpdateContext(jiraClient, cache)
    return Comment(
        body,
        author.toDomain(jiraClient),
        { getGroups(jiraClient, author.name).fold({ null }, { it }) },
        createdDate.toInstant(),
        updatedDate.toInstant(),
        visibility?.type,
        visibility?.value,
        ::restrictCommentToGroup.partially1(context).partially1(this).partially1("staff"),
        ::updateCommentBody.partially1(context).partially1(this)
    )
}

fun JiraUser.toDomain(jiraClient: JiraClient) = User(
    name, displayName,
    ::getUserGroups.partially1(jiraClient).partially1(name)
)

private fun getUserGroups(jiraClient: JiraClient, username: String) = getGroups(
    jiraClient,
    username
).fold({ null }, { it })

fun JiraIssue.toLinkedIssue(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config,
    cache: IssueUpdateContextCache
) = LinkedIssue(
    key,
    status.name,
    ::getFullIssue.partially1(jiraClient).partially1(messages).partially1(config).partially1(cache),
    ::createLink.partially1(getUpdateContext(jiraClient, cache))
)

fun JiraIssueLink.toDomain(
    jiraClient: JiraClient,
    issue: JiraIssue,
    messages: HelperMessages,
    config: Config,
    cache: IssueUpdateContextCache
) = Link(
    type.name,
    outwardIssue != null,
    (outwardIssue ?: inwardIssue).toLinkedIssue(jiraClient, messages, config, cache),
    ::deleteLink.partially1(issue.getUpdateContext(jiraClient, cache)).partially1(this)
)

fun JiraChangeLogItem.toDomain(jiraClient: JiraClient, entry: JiraChangeLogEntry) = ChangeLogItem(
    entry.created.toInstant(),
    field,
    from,
    fromString,
    to,
    toString,
    entry.author.toDomain(jiraClient),
    ::getUserGroups.partially1(jiraClient).partially1(entry.author.name)
)

private fun JiraIssue.mapLinks(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config,
    cache: IssueUpdateContextCache
) = issueLinks.map { it.toDomain(jiraClient, this, messages, config, cache) }

private fun JiraIssue.mapComments(jiraClient: JiraClient, cache: IssueUpdateContextCache) =
    comments.map { it.toDomain(jiraClient, this, cache) }

private fun JiraIssue.mapAttachments(jiraClient: JiraClient, cache: IssueUpdateContextCache) =
    attachments.map { it.toDomain(jiraClient, this, cache) }

private fun JiraIssue.mapVersions(jiraClient: JiraClient, cache: IssueUpdateContextCache) =
    versions.map { it.toDomain(jiraClient, this, cache) }

private fun JiraIssue.getChangeLogEntries(jiraClient: JiraClient) =
    changeLog.entries.flatMap { e ->
        e.items.map { i ->
            i.toDomain(jiraClient, e)
        }
    }

private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?

private fun JiraIssue.getCustomField(customField: String): String? =
    ((getField(customField)) as? JSONObject)?.get("value") as? String?

private fun JiraIssue.getEnvironment() = getFieldAsString("environment")

private fun JiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
private fun JiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
private fun JiraIssue.getLinked(config: Config) = getField(config[Arisa.CustomFields.linked]) as? Double?
private fun JiraIssue.getPriority(config: Config) = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
private fun JiraIssue.getTriagedTime(config: Config) = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")
private fun String.toVersionReleaseInstant() = versionDateFormat.parse(this).toInstant()

private fun JiraIssue.getFullIssue(
    jiraClient: JiraClient,
    messages: HelperMessages,
    config: Config,
    cache: IssueUpdateContextCache
): Either<Throwable, Issue> =
    getIssue(jiraClient, key).fold(
        { it.left() },
        { it.toDomain(jiraClient, jiraClient.getProject(it.project.key), messages, config, cache).right() }
    )
