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

class Mapper(
    private val jiraClient: JiraClient,
    private val config: Config,
    private val projectCache: ProjectCache,
    private val helperMessageService: HelperMessageService
) {
    private fun JiraAttachment.toDomain(issue: JiraIssue) = Attachment(
        id,
        fileName,
        getCreationDate(issue, id, issue.createdDate.toInstant()),
        mimeType,
        ::deleteAttachment.partially1(issue.getUpdateContext()).partially1(this),
        { openAttachmentStream(jiraClient, this) },
        this::download,
        author?.toDomain()
    )

    private fun getCreationDate(issue: JiraIssue, id: String, default: Instant) = issue.changeLog.entries
        .filter { it.items.any { item -> item.field == "Attachment" && item.to == id } }
        .maxByOrNull { it.created }
        ?.created
        ?.toInstant() ?: default

    private fun JiraProject.getSecurityLevelId() =
        config[Arisa.PrivateSecurityLevel.special][key] ?: config[Arisa.PrivateSecurityLevel.default]

    private fun JiraVersion.toDomain() = Version(
        id,
        name,
        isReleased,
        isArchived,
        releaseDate?.toVersionReleaseInstant()
    )

    private fun JiraIssue.getUpdateContext(): Lazy<IssueUpdateContext> =
        lazy {
            IssueUpdateContextCache.get(key) ?: IssueUpdateContext(
                jiraClient,
                this,
                update(),
                transition(),
                transition()
            ).also { IssueUpdateContextCache.add(key, it) }
        }

    @Suppress("LongMethod")
    fun toDomain(jiraIssue: JiraIssue): Issue {
        with(jiraIssue) {
            val project = projectCache.getProjectFromTicketId(key)
            val context = getUpdateContext()
            return Issue(
                key,
                summary,
                status.name,
                description,
                getEnvironment(),
                security?.id,
                reporter?.toDomain(),
                resolution?.name,
                createdDate.toInstant(),
                updatedDate.toInstant(),
                resolutionDate?.toInstant(),
                getCHK(),
                getConfirmation(),
                getLinked(),
                getMojangPriority(),
                getTriagedTime(),
                project.toDomain(),
                getPlatform(),
                getDungeonsPlatform(),
                mapVersions(),
                mapFixVersions(),
                mapAttachments(),
                mapComments(),
                mapLinks(),
                getChangeLogEntries(),
                ::reopen.partially1(context),
                ::resolveAs.partially1(context).partially1("Awaiting Response"),
                ::resolveAs.partially1(context).partially1("Invalid"),
                ::resolveAs.partially1(context).partially1("Duplicate"),
                ::resolveAs.partially1(context).partially1("Incomplete"),
                ::updateDescription.partially1(context),
                ::updateCHK.partially1(context).partially1(config[Arisa.CustomFields.chkField]),
                ::updateConfirmation.partially1(context).partially1(config[Arisa.CustomFields.confirmationField]),
                ::updatePlatform.partially1(context).partially1(config[Arisa.CustomFields.platformField]),
                ::updateDungeonsPlatform.partially1(context)
                    .partially1(config[Arisa.CustomFields.dungeonsPlatformField]),
                ::updateLinked.partially1(context).partially1(config[Arisa.CustomFields.linked]),
                ::updateSecurity.partially1(context).partially1(project.getSecurityLevelId()),
                ::addAffectedVersionById.partially1(context),
                { version -> addAffectedVersionById(context, version.id) },
                { version -> removeAffectedVersionById(context, version.id) },
                ::createLink.partially1(context).partially1 { key -> getOtherUpdateContext(key) },
                addComment = { (messageKey, variable, signatureMessageKey, language, restriction) ->
                    createComment(
                        context,
                        helperMessageService.getMessageWithBotSignature(
                            project = project.key,
                            key = messageKey,
                            filledText = variable,
                            signatureKey = signatureMessageKey,
                            lang = language
                        ),
                        restriction
                    )
                },
                addRawComment = ::createComment.partially1(context),
                ::markAsFixedWithSpecificVersion.partially1(context),
                ::changeReporter.partially1(context),
                ::addAttachmentFile.partially1(context)
            )
        }
    }

    private fun JiraProject.toDomain() = Project(
        key,
        versions.map { it.toDomain() },
        getSecurityLevelId()
    )

    private fun JiraComment.toDomain(issue: JiraIssue): Comment {
        val context = issue.getUpdateContext()
        return Comment(
            body,
            author.toDomain(),
            { getGroups(jiraClient, author.name).fold({ null }, { it }) },
            createdDate.toInstant(),
            updatedDate.toInstant(),
            visibility?.type,
            visibility?.value,
            ::restrictCommentToGroup.partially1(context).partially1(this).partially1("staff"),
            ::updateCommentBody.partially1(context).partially1(this)
        )
    }

    private fun JiraUser.toDomain() = User(
        name, displayName,
        ::getUserGroups.partially1(name),
        ::isNewUser.partially1(name)
    )

    private fun getUserGroups(username: String) = getGroups(
        jiraClient,
        username
    ).fold({ null }, { it })

    private fun isNewUser(username: String): Boolean {
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

    private fun JiraIssue.toLinkedIssue() = LinkedIssue(
        key,
        status.name,
        { getFullIssue() },
        ::createLink.partially1(getUpdateContext()).partially1 { key -> getOtherUpdateContext(key) }
    )

    private fun JiraIssueLink.toDomain(issue: JiraIssue) = Link(
        type.name,
        outwardIssue != null,
        (outwardIssue ?: inwardIssue).toLinkedIssue(),
        ::deleteLink.partially1(issue.getUpdateContext()).partially1(this)
    )

    private fun JiraChangeLogItem.toDomain(entry: JiraChangeLogEntry) = ChangeLogItem(
        entry.created.toInstant(),
        field,
        from,
        fromString,
        to,
        toString,
        entry.author.toDomain(),
        ::getUserGroups.partially1(entry.author.name)
    )

    private fun JiraIssue.mapLinks() = issueLinks.map {
        it.toDomain(this)
    }

    private fun JiraIssue.mapComments() =
        comments.map { it.toDomain(this) }

    private fun JiraIssue.mapAttachments() =
        attachments.map { it.toDomain(this) }

    private fun JiraIssue.mapVersions() =
        versions.map { it.toDomain() }

    private fun JiraIssue.mapFixVersions() =
        fixVersions.map { it.toDomain() }

    private fun JiraIssue.getChangeLogEntries() =
        changeLog.entries.flatMap { e ->
            e.items.map { i -> i.toDomain(e) }
        }

    private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?

    private fun JiraIssue.getCustomField(customField: String): String? =
        ((getField(customField)) as? JSONObject)?.get("value") as? String?

    private fun JiraIssue.getEnvironment() = getFieldAsString("environment")

    private fun JiraIssue.getCHK() = getFieldAsString(config[Arisa.CustomFields.chkField])
    private fun JiraIssue.getConfirmation() = getCustomField(config[Arisa.CustomFields.confirmationField])
    private fun JiraIssue.getDungeonsPlatform() =
        getCustomField(config[Arisa.CustomFields.dungeonsPlatformField])

    private fun JiraIssue.getLinked() = getField(config[Arisa.CustomFields.linked]) as? Double?
    private fun JiraIssue.getMojangPriority() = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
    private fun JiraIssue.getTriagedTime() = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
    private fun JiraIssue.getPlatform() = getCustomField(config[Arisa.CustomFields.platformField])
    private val versionDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private fun String.toVersionReleaseInstant() = versionDateFormat.parse(this).toInstant()

    private fun JiraIssue.getFullIssue(): Either<Throwable, Issue> =
        getIssue(jiraClient, key).fold(
            { it.left() },
            { toDomain(it).right() }
        )

    // run with Either.catch {}!
    private fun JiraIssue.getOtherUpdateContext(key: String): Lazy<IssueUpdateContext> =
        lazy {
            IssueUpdateContextCache.get(key) ?: IssueUpdateContext(
                jiraClient,
                jiraClient.getIssue(key),
                update(),
                transition(),
                transition()
            ).also { IssueUpdateContextCache.add(key, it) }
        }
}
