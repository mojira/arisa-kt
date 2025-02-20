@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import com.uchuhimo.konf.Config
import com.urielsalis.mccrashlib.deobfuscator.getSafeChildPath
// import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.domain.cloud.CloudAttachment
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.domain.cloud.CloudLink
import io.github.mojira.arisa.domain.cloud.CloudLinkedIssue
import io.github.mojira.arisa.infrastructure.HelperMessageService
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.ProjectCache
import io.github.mojira.arisa.infrastructure.apiclient.models.Changelog
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import net.rcarz.jiraclient.JiraClient
import io.github.mojira.arisa.infrastructure.apiclient.JiraClient as MojiraClient
import net.rcarz.jiraclient.JiraException
import net.sf.json.JSONObject
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.Instant
import net.rcarz.jiraclient.Attachment as JiraAttachment
import io.github.mojira.arisa.infrastructure.apiclient.models.AttachmentBean as MojiraAttachment
import net.rcarz.jiraclient.ChangeLogEntry as JiraChangeLogEntry
import io.github.mojira.arisa.infrastructure.apiclient.models.Changelog as MojiraChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem as JiraChangeLogItem
import io.github.mojira.arisa.infrastructure.apiclient.models.ChangeDetails as MojiraChangeLogItem
import net.rcarz.jiraclient.Comment as JiraComment
import io.github.mojira.arisa.infrastructure.apiclient.models.Comment as MojiraComment
import net.rcarz.jiraclient.Issue as JiraIssue
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueBean as MojiraIssue
import net.rcarz.jiraclient.IssueLink as JiraIssueLink
import io.github.mojira.arisa.infrastructure.apiclient.models.LinkedIssue as MojiraLinkedIssue
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueLink as MojiraIssueLink
import net.rcarz.jiraclient.Project as JiraProject
import io.github.mojira.arisa.infrastructure.apiclient.models.Project as MojiraProject
import net.rcarz.jiraclient.User as JiraUser
import io.github.mojira.arisa.infrastructure.apiclient.models.UserDetails as MojiraUserDetails
import net.rcarz.jiraclient.Version as JiraVersion
import io.github.mojira.arisa.infrastructure.apiclient.models.Version as MojiraVersion


fun MojiraAttachment.toDomain(jiraClient: MojiraClient, issue: MojiraIssue, config: Config) = CloudAttachment(
    id = id,
    filename = filename,
    created = getCreationDate(issue, id, issue.fields.created?.toInstant() ?: Instant.now()),
    mimeType = mimeType,
    remove = ::deleteAttachment.partially1(issue.getUpdateContext(jiraClient)).partially1(this),
    openContentStream = { openAttachmentStream(jiraClient, this) },
    // Cache attachment content once it has been downloaded
    getContent = lazy { jiraClient.downloadAttachment(id) }::value,
    uploader = author?.toDomain(jiraClient, config)
)

fun getCreationDate(issue: MojiraIssue, id: String, default: Instant) = (issue.changelog.histories as List<Changelog>)
    .filter { changeLogEntry -> changeLogEntry.items.any { it.field == "Attachment" && it.to == id } }
    .maxByOrNull { it.created }
    ?.created
    ?.toInstant() ?: default

fun MojiraProject.getSecurityLevelId(config: Config) = config[Arisa.PrivateSecurityLevel.default]

fun MojiraVersion.toDomain() = Version(
    id,
    name,
    isReleased,
    isArchived,
    releaseDate?.toInstant()
)

fun MojiraIssue.getUpdateContext(jiraClient: MojiraClient): Lazy<IssueUpdateContext> =
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
fun MojiraIssue.toDomain(
    jiraClient: MojiraClient,
    project: MojiraProject,
    config: Config
): CloudIssue {
    val context = getUpdateContext(jiraClient)
    val addAttachmentFromFile = ::addAttachmentFile.partially1(context)
    return CloudIssue(
        key = key,
        summary = fields.summary,
        status = fields.status.name,
        description = fields.description,
        environment = fields.environment,
        securityLevel = fields.security?.id,
//        reporter?.toDomain(jiraClient, config),
        resolution = fields.resolution?.name,
        created = fields.created?.toInstant() ?: Instant.now(),
//        updatedDate.toInstant(),
//        resolutionDate?.toInstant(),
//        getCHK(config),
//        getConfirmation(config),
//        getLinked(config),
//        getPriority(config),
//        getTriagedTime(config),
        project = project.toDomain(config),
//        getPlatform(config),
//        getDungeonsPlatform(config),
//        getLegendsPlatform(config),
//        mapVersions(),
//        mapFixVersions(),
        attachments = mapAttachments(jiraClient, config),
        comments = mapComments(jiraClient, config),
        links = mapLinks(jiraClient, config),
        changeLog = getChangeLogEntries(jiraClient, config),
//        ::reopen.partially1(context),
//        ::resolveAs.partially1(context).partially1("Awaiting Response"),
//        ::resolveAs.partially1(context).partially1("Invalid"),
//        ::resolveAs.partially1(context).partially1("Duplicate"),
//        ::resolveAs.partially1(context).partially1("Incomplete"),
//        ::updateDescription.partially1(context),
//        ::updateCHK.partially1(context).partially1(config[Arisa.CustomFields.chkField]),
//        ::updateConfirmation.partially1(context).partially1(config[Arisa.CustomFields.confirmationField]),
//        ::updatePriority.partially1(context).partially1(config[Arisa.CustomFields.mojangPriorityField]),
//        ::updatePlatform.partially1(context).partially1(config[Arisa.CustomFields.platformField]),
//        ::updateDungeonsPlatform.partially1(context).partially1(config[Arisa.CustomFields.dungeonsPlatformField]),
//        ::updateLegendsPlatform.partially1(context).partially1(config[Arisa.CustomFields.legendsPlatformField]),
//        ::updateLinked.partially1(context).partially1(config[Arisa.CustomFields.linked]),
        setPrivate = ::updateSecurity.partially1(context).partially1(project.getSecurityLevelId(config)),
//        ::addAffectedVersionById.partially1(context),
//        { version -> addAffectedVersionById(context, version.id) },
//        { version -> removeAffectedVersionById(context, version.id) },
//        ::createLink.partially1(context).partially1(::getOtherUpdateContext.partially1(jiraClient)),
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
//        addDupeMessage = { (messageKey, variable, language) ->
//            createComment(
//                context,
//                HelperMessageService.getMessageWithDupeBotSignature(
//                    project.key,
//                    messageKey,
//                    variable,
//                    language
//                )
//            )
//        },
//        addRestrictedComment = { (messageKey, variable, language) ->
//            addRestrictedComment(
//                context,
//                HelperMessageService.getMessageWithBotSignature(
//                    project.key,
//                    messageKey,
//                    variable,
//                    language
//                ),
//                "helper"
//            )
//        },
//        addNotEnglishComment = { language ->
//            createComment(
//                context,
//                HelperMessageService.getMessageWithBotSignature(
//                    project.key,
//                    config[Arisa.Modules.Language.message],
//                    lang = language
//                )
//            )
//        },
        addRawRestrictedComment = ::addRestrictedComment.partially1(context),
        addRawBotComment = { rawMessage ->
            createComment(
                context,
                HelperMessageService.getRawMessageWithBotSignature(rawMessage)
            )
        },
//        ::markAsFixedWithSpecificVersion.partially1(context),
//        ::changeReporter.partially1(context),
        addAttachmentFromFile,
//        addAttachment = { name, content ->
//            val tempDir = Files.createTempDirectory("arisa-attachment-upload").toFile()
//            val safePath = getSafeChildPath(tempDir, name)
//            if (safePath == null) {
//                tempDir.delete()
//                throw IllegalArgumentException("Cannot create safe path name for '${sanitizeCommentArg(name)}'")
//            } else {
//                safePath.writeText(content)
//                addAttachmentFromFile(safePath) {
//                    // Once uploaded, delete the temp directory containing the attachment
//                    tempDir.deleteRecursively()
//                }
//            }
//        }
    )
}

fun MojiraProject.toDomain(
    config: Config
) = Project(
    key = key,
    versions = versions.map { it.toDomain() },
    privateSecurity = getSecurityLevelId(config)
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
fun MojiraIssue.toLinkedIssue(
    jiraClient: MojiraClient,
    config: Config
) = CloudLinkedIssue(
    key = key,
    status = fields.status.name,
    getFullIssue = { getFullIssue(jiraClient, config) },
    createLink = ::createLink.partially1(getUpdateContext(jiraClient)).partially1(
        ::getOtherUpdateContext
            .partially1(jiraClient)
    )
)

@Suppress("LongParameterList")
fun MojiraIssueLink.toDomain(
    jiraClient: MojiraClient,
    issue: MojiraIssue,
    config: Config
) = CloudLink(
    type = type?.name!!,
    outwards = outwardIssue != null,
    issue = (outwardIssue ?: inwardIssue)!!.toLinkedIssue(
        jiraClient,
        config
    ),
    remove = ::deleteLink.partially1(issue.getUpdateContext(jiraClient)).partially1(this)
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
private fun MojiraIssue.mapLinks(
    jiraClient: MojiraClient,
    config: Config
) = fields.issuelinks.map {
    it.toDomain(jiraClient, this, config)
}

private fun MojiraIssue.mapComments(jiraClient: MojiraClient, config: Config) =
    fields.comment?.comments?.map { it.toDomain(jiraClient, this, config) } ?: emptyList()

private fun MojiraIssue.mapAttachments(jiraClient: MojiraClient, config: Config) =
    fields.attachment.map { it.toDomain(jiraClient, this, config) }

//private fun MojiraIssue.mapVersions() =
//    fields.versions.map { it.toDomain() }
//
//private fun MojiraIssue.mapFixVersions() =
//    fixVersions.map { it.toDomain() }

private fun MojiraIssue.getChangeLogEntries(jiraClient: MojiraClient, config: Config) =
    (changelog.histories as List<Changelog>).flatMap { e: Changelog ->
        e.items.mapIndexed { index, item ->
            item.toDomain(jiraClient, e, index, config)
        }
    }

//private fun MojiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?
//
//private fun MojiraIssue.getCustomField(customField: String): String? =
//    ((getField(customField)) as? JSONObject)?.get("value") as? String?

//private fun MojiraIssue.getEnvironment() = getFieldAsString("environment")
//
//private fun MojiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
//private fun MojiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
//private fun MojiraIssue.getDungeonsPlatform(config: Config) =
//    getCustomField(config[Arisa.CustomFields.dungeonsPlatformField])
//private fun MojiraIssue.getLegendsPlatform(config: Config) =
//    getCustomField(config[Arisa.CustomFields.legendsPlatformField])
//
//private fun JiraIssue.getLinked(config: Config) = getField(config[Arisa.CustomFields.linked]) as? Double?
//private fun MojiraIssue.getPriority(config: Config) = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
//private fun MojiraIssue.getTriagedTime(config: Config) = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
//private fun MojiraIssue.getPlatform(config: Config) = getCustomField(config[Arisa.CustomFields.platformField])
private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")
private fun String.toVersionReleaseInstant() = versionDateFormat.parse(this).toInstant()

@Suppress("LongParameterList")
private fun MojiraIssue.getFullIssue(
    jiraClient: MojiraClient,
    config: Config
): Either<Throwable, CloudIssue> =
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
private fun MojiraIssue.getOtherUpdateContext(
    jiraClient: MojiraClient,
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
