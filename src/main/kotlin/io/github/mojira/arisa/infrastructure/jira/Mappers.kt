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
import io.github.mojira.arisa.infrastructure.HelperMessageService
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.ProjectCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException
import net.sf.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import net.rcarz.jiraclient.Attachment as JiraAttachment
import net.rcarz.jiraclient.ChangeLogEntry as JiraChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem as JiraChangeLogItem
import net.rcarz.jiraclient.Comment as JiraComment
import net.rcarz.jiraclient.Issue as JiraIssue
import net.rcarz.jiraclient.IssueLink as JiraIssueLink
import net.rcarz.jiraclient.Project as JiraProject
import net.rcarz.jiraclient.User as JiraUser
import net.rcarz.jiraclient.Version as JiraVersion

fun JiraAttachment.toDomain(jiraClient: JiraClient, issue: JiraIssue, config: Config) = Attachment(
    id,
    fileName,
    getCreationDate(issue, id, issue.createdDate.toInstant()),
    mimeType,
    ::deleteAttachment.partially1(issue.getUpdateContext(jiraClient)).partially1(this),
    { openAttachmentStream(jiraClient, this) },
    // Cache attachment content once it has been downloaded
    lazy { this.download() }::value,
    author?.toDomain(jiraClient, config)
)

fun getCreationDate(issue: JiraIssue, id: String, default: Instant) = issue.changeLog.entries
    .filter { it.items.any { it.field == "Attachment" && it.to == id } }
    .maxByOrNull { it.created }
    ?.created
    ?.toInstant() ?: default

fun JiraProject.getSecurityLevelId(config: Config) =
    config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]

fun JiraVersion.toDomain() = Version(
    id,
    name,
    isReleased,
    isArchived,
    releaseDate?.toVersionReleaseInstant()
)

fun JiraIssue.getUpdateContext(jiraClient: JiraClient): Lazy<IssueUpdateContext> =
    lazy {
        IssueUpdateContextCache.get(key) ?: IssueUpdateContext(
            jiraClient,
            this,
            update(),
            transition(),
            transition()
        ).also { IssueUpdateContextCache.add(key, it) }
    }

@Suppress("LongMethod", "LongParameterList")
fun JiraIssue.toDomain(
    jiraClient: JiraClient,
    project: JiraProject,
    config: Config
): Issue {
    val context = getUpdateContext(jiraClient)
    return Issue(
        key,
        summary,
        status.name,
        description,
        getEnvironment(),
        security?.id,
        reporter?.toDomain(jiraClient, config),
        resolution?.name,
        createdDate.toInstant(),
        updatedDate.toInstant(),
        resolutionDate?.toInstant(),
        getCHK(config),
        getConfirmation(config),
        getLinked(config),
        getPriority(config),
        getTriagedTime(config),
        project.toDomain(config),
        getPlatform(config),
        getDungeonsPlatform(config),
        mapVersions(),
        mapFixVersions(),
        mapAttachments(jiraClient, config),
        mapComments(jiraClient, config),
        mapLinks(jiraClient, config),
        getChangeLogEntries(jiraClient, config),
        ::reopen.partially1(context),
        ::resolveAs.partially1(context).partially1("Awaiting Response"),
        ::resolveAs.partially1(context).partially1("Invalid"),
        ::resolveAs.partially1(context).partially1("Duplicate"),
        ::resolveAs.partially1(context).partially1("Incomplete"),
        ::updateDescription.partially1(context),
        ::updateCHK.partially1(context).partially1(config[Arisa.CustomFields.chkField]),
        ::updateConfirmation.partially1(context).partially1(config[Arisa.CustomFields.confirmationField]),
        ::updatePlatform.partially1(context).partially1(config[Arisa.CustomFields.platformField]),
        ::updateDungeonsPlatform.partially1(context).partially1(config[Arisa.CustomFields.dungeonsPlatformField]),
        ::updateLinked.partially1(context).partially1(config[Arisa.CustomFields.linked]),
        ::updateSecurity.partially1(context).partially1(project.getSecurityLevelId(config)),
        ::addAffectedVersionById.partially1(context),
        { version -> addAffectedVersionById(context, version.id) },
        { version -> removeAffectedVersionById(context, version.id) },
        ::createLink.partially1(context).partially1(::getOtherUpdateContext.partially1(jiraClient)),
        addComment = { (messageKey, variable, language) ->
            createComment(
                context,
                HelperMessageService.getMessageWithBotSignature(
                    project.key, messageKey, variable, language
                )
            )
        },
        addDupeMessage = { (messageKey, variable, language) ->
            createComment(
                context,
                HelperMessageService.getMessageWithDupeBotSignature(
                    project.key, messageKey, variable, language
                )
            )
        },
        addRestrictedComment = { (messageKey, variable, language) ->
            addRestrictedComment(
                context,
                HelperMessageService.getMessageWithBotSignature(
                    project.key, messageKey, variable, language
                ),
                "helper"
            )
        },
        addNotEnglishComment = { language ->
            createComment(
                context,
                HelperMessageService.getMessageWithBotSignature(
                    project.key, config[Arisa.Modules.Language.message], lang = language
                )
            )
        },
        addRawRestrictedComment = ::addRestrictedComment.partially1(context),
        addRawBotComment = { rawMessage ->
            createComment(
                context,
                HelperMessageService.getRawMessageWithBotSignature(rawMessage)
            )
        },
        ::markAsFixedWithSpecificVersion.partially1(context),
        ::changeReporter.partially1(context),
        ::addAttachmentFile.partially1(context)
    )
}

fun JiraProject.toDomain(
    config: Config
) = Project(
    key,
    versions.map { it.toDomain() },
    getSecurityLevelId(config)
)

fun JiraComment.toDomain(
    jiraClient: JiraClient,
    issue: JiraIssue,
    config: Config
): Comment {
    val context = issue.getUpdateContext(jiraClient)
    return Comment(
        id,
        body,
        author.toDomain(jiraClient, config),
        { getGroups(jiraClient, author.name).fold({ null }, { it }) },
        createdDate.toInstant(),
        updatedDate.toInstant(),
        visibility?.type,
        visibility?.value,
        ::restrictCommentToGroup.partially1(context).partially1(this).partially1("staff"),
        ::updateCommentBody.partially1(context).partially1(this)
    )
}

fun JiraUser.toDomain(jiraClient: JiraClient, config: Config) = User(
    name, displayName,
    ::getUserGroups.partially1(jiraClient).partially1(name),
    ::isNewUser.partially1(jiraClient).partially1(name),
    // Check case insensitively because it apparently does not matter when logging in, so `username` might have
    // incorrect capitalization
    { name.equals(config[Arisa.Credentials.username], ignoreCase = true) }
)

private fun getUserGroups(jiraClient: JiraClient, username: String) = getGroups(
    jiraClient,
    username
).fold({ null }, { it })

private fun isNewUser(jiraClient: JiraClient, username: String): Boolean {
    val commentJql = "issueFunction IN commented(${ escapeIssueFunction(username) { "by $it before -24h" } })"

    val oldCommentsExist = try {
        jiraClient.countIssues(commentJql) > 0
    } catch (_: JiraException) { false }

    if (oldCommentsExist) return false

    val reportJql = """project != TRASH AND reporter = '${username.replace("'", "\\'")}' AND created < -24h"""

    val oldReportsExist = try {
        jiraClient.countIssues(reportJql) > 0
    } catch (_: JiraException) { true }

    return !oldReportsExist
}

@Suppress("LongParameterList")
fun JiraIssue.toLinkedIssue(
    jiraClient: JiraClient,
    config: Config
) = LinkedIssue(
    key,
    status.name,
    { getFullIssue(jiraClient, config) },
    ::createLink.partially1(getUpdateContext(jiraClient)).partially1(::getOtherUpdateContext
            .partially1(jiraClient))
)

@Suppress("LongParameterList")
fun JiraIssueLink.toDomain(
    jiraClient: JiraClient,
    issue: JiraIssue,
    config: Config
) = Link(
    type.name,
    outwardIssue != null,
    (outwardIssue ?: inwardIssue).toLinkedIssue(
        jiraClient,
        config
    ),
    ::deleteLink.partially1(issue.getUpdateContext(jiraClient)).partially1(this)
)

fun JiraChangeLogItem.toDomain(
    jiraClient: JiraClient,
    entry: JiraChangeLogEntry,
    itemIndex: Int,
    config: Config
) = ChangeLogItem(
    entry.id,
    itemIndex,
    entry.created.toInstant(),
    field,
    from,
    fromString,
    to,
    toString,
    entry.author.toDomain(jiraClient, config),
    ::getUserGroups.partially1(jiraClient).partially1(entry.author.name)
)

@Suppress("LongParameterList")
private fun JiraIssue.mapLinks(
    jiraClient: JiraClient,
    config: Config
) = issueLinks.map {
    it.toDomain(jiraClient, this, config)
}

private fun JiraIssue.mapComments(jiraClient: JiraClient, config: Config) =
    comments.map { it.toDomain(jiraClient, this, config) }

private fun JiraIssue.mapAttachments(jiraClient: JiraClient, config: Config) =
    attachments.map { it.toDomain(jiraClient, this, config) }

private fun JiraIssue.mapVersions() =
    versions.map { it.toDomain() }

private fun JiraIssue.mapFixVersions() =
    fixVersions.map { it.toDomain() }

private fun JiraIssue.getChangeLogEntries(jiraClient: JiraClient, config: Config) =
    changeLog.entries.flatMap { e ->
        e.items.mapIndexed { index, item ->
            item.toDomain(jiraClient, e, index, config)
        }
    }

private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?

private fun JiraIssue.getCustomField(customField: String): String? =
    ((getField(customField)) as? JSONObject)?.get("value") as? String?

private fun JiraIssue.getEnvironment() = getFieldAsString("environment")

private fun JiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
private fun JiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
private fun JiraIssue.getDungeonsPlatform(config: Config) =
    getCustomField(config[Arisa.CustomFields.dungeonsPlatformField])
private fun JiraIssue.getLinked(config: Config) = getField(config[Arisa.CustomFields.linked]) as? Double?
private fun JiraIssue.getPriority(config: Config) = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
private fun JiraIssue.getTriagedTime(config: Config) = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
private fun JiraIssue.getPlatform(config: Config) = getCustomField(config[Arisa.CustomFields.platformField])
private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")
private fun String.toVersionReleaseInstant() = versionDateFormat.parse(this).toInstant()

@Suppress("LongParameterList")
private fun JiraIssue.getFullIssue(
    jiraClient: JiraClient,
    config: Config
): Either<Throwable, Issue> =
    getIssue(jiraClient, key).fold(
        { it.left() },
        {
            it.toDomain(
                jiraClient,
                ProjectCache.getProjectFromTicketId(it.key),
                config
            ).right()
        }
    )

// run with Either.catch {}!
private fun JiraIssue.getOtherUpdateContext(
    jiraClient: JiraClient,
    key: String
): Lazy<IssueUpdateContext> =
    lazy {
        IssueUpdateContextCache.get(key) ?: IssueUpdateContext(
            jiraClient,
            jiraClient.getIssue(key),
            update(),
            transition(),
            transition()
        ).also { IssueUpdateContextCache.add(key, it) }
    }
