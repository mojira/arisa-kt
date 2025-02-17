@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import com.uchuhimo.konf.Config
import com.urielsalis.mccrashlib.deobfuscator.getSafeChildPath
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
import io.github.mojira.arisa.infrastructure.apiclient.models.Changelog
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException
import net.sf.json.JSONObject
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.Instant
import net.rcarz.jiraclient.Attachment as JiraAttachment
import net.rcarz.jiraclient.ChangeLogEntry as JiraChangeLogEntry
import io.github.mojira.arisa.infrastructure.apiclient.models.Changelog as MojiraChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem as JiraChangeLogItem
import io.github.mojira.arisa.infrastructure.apiclient.models.ChangeDetails as MojiraChangeLogItem
import net.rcarz.jiraclient.Comment as JiraComment
import io.github.mojira.arisa.infrastructure.apiclient.models.Comment as MojiraComment
import net.rcarz.jiraclient.Issue as JiraIssue
import net.rcarz.jiraclient.IssueLink as JiraIssueLink
import net.rcarz.jiraclient.Project as JiraProject
import net.rcarz.jiraclient.User as JiraUser
import io.github.mojira.arisa.infrastructure.apiclient.models.UserDetails as MojiraUserDetails
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
    .filter { changeLogEntry -> changeLogEntry.items.any { it.field == "Attachment" && it.to == id } }
    .maxByOrNull { it.created }
    ?.created
    ?.toInstant() ?: default

fun JiraProject.getSecurityLevelId(config: Config) = config[Arisa.PrivateSecurityLevel.default]

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
    val addAttachmentFromFile = ::addAttachmentFile.partially1(context)
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
        getLegendsPlatform(config),
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
        ::updatePriority.partially1(context).partially1(config[Arisa.CustomFields.mojangPriorityField]),
        ::updatePlatform.partially1(context).partially1(config[Arisa.CustomFields.platformField]),
        ::updateDungeonsPlatform.partially1(context).partially1(config[Arisa.CustomFields.dungeonsPlatformField]),
        ::updateLegendsPlatform.partially1(context).partially1(config[Arisa.CustomFields.legendsPlatformField]),
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
                    project.key,
                    messageKey,
                    variable,
                    language
                )
            )
        },
        addDupeMessage = { (messageKey, variable, language) ->
            createComment(
                context,
                HelperMessageService.getMessageWithDupeBotSignature(
                    project.key,
                    messageKey,
                    variable,
                    language
                )
            )
        },
        addRestrictedComment = { (messageKey, variable, language) ->
            addRestrictedComment(
                context,
                HelperMessageService.getMessageWithBotSignature(
                    project.key,
                    messageKey,
                    variable,
                    language
                ),
                "helper"
            )
        },
        addNotEnglishComment = { language ->
            createComment(
                context,
                HelperMessageService.getMessageWithBotSignature(
                    project.key,
                    config[Arisa.Modules.Language.message],
                    lang = language
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
        addAttachmentFromFile,
        addAttachment = { name, content ->
            val tempDir = Files.createTempDirectory("arisa-attachment-upload").toFile()
            val safePath = getSafeChildPath(tempDir, name)
            if (safePath == null) {
                tempDir.delete()
                throw IllegalArgumentException("Cannot create safe path name for '${sanitizeCommentArg(name)}'")
            } else {
                safePath.writeText(content)
                addAttachmentFromFile(safePath) {
                    // Once uploaded, delete the temp directory containing the attachment
                    tempDir.deleteRecursively()
                }
            }
        }
    )
}

fun JiraProject.toDomain(
    config: Config
) = Project(
    key,
    versions.map { it.toDomain() },
    getSecurityLevelId(config)
)

fun MojiraComment.toDomain(
    jiraClient: MojiraClient,
    issue: MojiraIssue,
    config: Config
): Comment {
    val context = issue.getUpdateContext(jiraClient)
    return Comment(
        id = id,
        body = body,
        author = author?.toDomain(jiraClient, config),
        updateAuthor = updateAuthor?.toDomain(jiraClient, config),
        getAuthorGroups = { getGroups(jiraClient, author!!.accountId).fold({ null }, { it }) },
        getUpdateAuthorGroups = { if (updateAuthor == null) emptyList() else getGroups(jiraClient, updateAuthor.accountId).fold({ null }, { it }) },
        created = created!!.toInstant(),
        updated = updated!!.toInstant(),
        visibilityType = visibility?.type.toString(),
        visibilityValue = visibility?.value,
        restrict = ::restrictCommentToGroup.partially1(context).partially1(this).partially1("staff"),
        update = ::updateCommentBody.partially1(context).partially1(this),
        remove = ::deleteComment.partially1(issue.getUpdateContext(jiraClient)).partially1(this)
    )
}

fun MojiraUserDetails.toDomain(jiraClient: MojiraClient, config: Config) = User(
    accountId = accountId,
    name = displayName,
    displayName = displayName,
    getGroups = ::getUserGroups.partially1(jiraClient).partially1(accountId),
    isNewUser = { false }
) {
    accountId.equals(config[Arisa.Credentials.accountId], ignoreCase = true)
}

private fun getUserGroups(jiraClient: MojiraClient, accountId: String) = getGroups(
    jiraClient,
    accountId
).fold({ null }, { it })

private fun isNewUser(jiraClient: JiraClient, username: String): Boolean {
    val commentJql = "issueFunction IN commented(${escapeIssueFunction(username) { "by $it before -24h" }})"

    val oldCommentsExist = try {
        jiraClient.countIssues(commentJql) > 0
    } catch (_: JiraException) {
        false
    }

    if (oldCommentsExist) return false

    val reportJql = """project != TRASH AND reporter = '${username.replace("'", "\\'")}' AND created < -24h"""

    val oldReportsExist = try {
        jiraClient.countIssues(reportJql) > 0
    } catch (_: JiraException) {
        true
    }

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
    ::createLink.partially1(getUpdateContext(jiraClient)).partially1(
        ::getOtherUpdateContext
            .partially1(jiraClient)
    )
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

fun MojiraChangeLogItem.toDomain(
    jiraClient: MojiraClient,
    entry: MojiraChangeLogEntry,
    itemIndex: Int,
    config: Config
) = ChangeLogItem(
    entryId = entry.id!!,
    itemIndex = itemIndex,
    created = entry.created.toInstant(),
    field = field,
    changedFrom = from,
    changedFromString = fromString,
    changedTo = to,
    changedToString = toString,
    author = entry.author!!.toDomain(jiraClient, config),
    getAuthorGroups = ::getUserGroups.partially1(jiraClient).partially1(entry.author.accountId)
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

private fun MojiraIssue.getChangeLogEntries(jiraClient: MojiraClient, config: Config) =
    (changelog.histories as List<Changelog>).flatMap { e: Changelog ->
        e.items.mapIndexed { index, item ->
            item.toDomain(jiraClient, e, index, config)
        }
    }

private fun JiraIssue.getCustomField(customField: String): String? =
    ((getField(customField)) as? JSONObject)?.get("value") as? String?

private fun JiraIssue.getEnvironment() = getFieldAsString("environment")

private fun JiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
private fun JiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
private fun JiraIssue.getDungeonsPlatform(config: Config) =
    getCustomField(config[Arisa.CustomFields.dungeonsPlatformField])
private fun JiraIssue.getLegendsPlatform(config: Config) =
    getCustomField(config[Arisa.CustomFields.legendsPlatformField])

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
