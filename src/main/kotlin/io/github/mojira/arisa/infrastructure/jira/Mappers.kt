package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.pipe
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkParam
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Version
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
import net.rcarz.jiraclient.Version as JiraVersion

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
private fun String.toInstant() = isoFormat.parse(this).toInstant()

private fun JiraIssue.getFieldAsString(field: String) = this.getField(field) as? String?

private fun JiraIssue.getCustomField(customField: String): String? =
    ((getField(customField)) as? JSONObject)?.get("value") as? String?

fun JiraIssue.getEnvironment() = getFieldAsString("environment")

fun JiraIssue.getCHK(config: Config) = getFieldAsString(config[Arisa.CustomFields.chkField])
fun JiraIssue.getConfirmation(config: Config) = getCustomField(config[Arisa.CustomFields.confirmationField])
fun JiraIssue.getLinked(config: Config) = getField(config[Arisa.CustomFields.linked]) as? Double?
fun JiraIssue.getPriority(config: Config) = getCustomField(config[Arisa.CustomFields.mojangPriorityField])
fun JiraIssue.getTriagedTime(config: Config) = getFieldAsString(config[Arisa.CustomFields.triagedTimeField])
fun JiraIssue.getCreated() = getFieldAsString("created")!!.toInstant()
fun JiraIssue.getUpdated() = getFieldAsString("created")!!.toInstant()

fun JiraAttachment.toDomain(remove: (JiraAttachment) -> Either<Throwable, Unit>) = Attachment(
    fileName, createdDate, remove.partially1(this), this::download
)

fun JiraIssue.getSecurityLevelId(config: Config) =
    config[Arisa.PrivateSecurityLevel.special][project.key] ?: config[Arisa.PrivateSecurityLevel.default]

fun JiraIssue.getAttachments(remove: (JiraAttachment) -> Either<Throwable, Unit>) =
    attachments.map { it.toDomain(remove) }

fun JiraVersion.toDomain(execute: (JiraVersion) -> Either<Throwable, Unit>) = Version(
    isReleased,
    isArchived,
    execute.partially1(this)
)

fun JiraIssue.getVersions(execute: (JiraVersion) -> Either<Throwable, Unit>) = versions.map { it.toDomain(execute) }
fun JiraProject.getVersions(execute: (JiraVersion) -> Either<Throwable, Unit>) = versions.map { it.toDomain(execute) }

fun JiraComment.toDomain(
    getGroups: (String) -> List<String>?,
    restrict: (JiraComment, String, String) -> Either<Throwable, Unit>,
    update: (JiraComment, String) -> Either<Throwable, Unit>
) = Comment(
    body,
    author.displayName,
    getGroups.partially1(author.name),
    createdDate.toInstant(),
    updatedDate.toInstant(),
    visibility?.type,
    visibility?.value,
    restrict.partially1(this).partially1("staff"),
    update.partially1(this)
)

private fun getUserGroups(jiraClient: JiraClient, username: String) = getGroups(
    jiraClient,
    username
).fold({ null }, { it })

fun JiraIssue.getComments(
    jiraClient: JiraClient
) = comments.map { it.toDomain(::getUserGroups.partially1(jiraClient), ::restrictCommentToGroup, ::updateCommentBody) }

fun <FIELD, FUNPARAM> JiraIssue.toLinkedIssue(
    jiraClient: JiraClient,
    setField: (JiraIssue, FUNPARAM) -> Either<Throwable, Unit>,
    getField: (JiraIssue) -> Either<Throwable, FIELD>
) = LinkedIssue(
    key,
    status.name,
    setField.partially1(this),
    {
        getIssue(
            jiraClient,
            key
        ) pipe { issueEither ->
            issueEither.fold(
                { it.left() },
                { getField(it) }
            )
        }
    }
)

fun getVersionsGetField(issue: JiraIssue) = issue.versions.map { it.id }.right()

fun <FIELD, FUNPARAM> JiraIssueLink.toDomain(
    jiraClient: JiraClient,
    setField: (JiraIssue, FUNPARAM) -> Either<Throwable, Unit>,
    getField: (JiraIssue) -> Either<Throwable, FIELD>
) = Link(
    type.name,
    outwardIssue != null,
    (outwardIssue ?: inwardIssue).toLinkedIssue(jiraClient, setField, getField),
    ::deleteLink.partially1(this)
)

fun <FIELD, FUNPARAM> JiraIssue.getLinks(
    jiraClient: JiraClient,
    setField: (JiraIssue, FUNPARAM) -> Either<Throwable, Unit>,
    getField: (JiraIssue) -> Either<Throwable, FIELD>
) = issueLinks.map { it.toDomain(jiraClient, setField, getField) }

fun createLinkForTransfer(jiraIssue: JiraIssue, linkParam: LinkParam) = createLink(
    jiraIssue,
    linkParam.type,
    linkParam.issue
)

fun createLinkParam(issue: JiraIssue) = LinkedIssue(
    issue.key,
    issue.status.name,
    ::createLinkForTransfer.partially1(issue),
    { UnsupportedOperationException().left() }
).right()

fun getIssueForLink(jiraClient: JiraClient, issue: JiraIssue) =
    issue.getLinks(jiraClient, ::createLinkForTransfer, ::createLinkParam).right()

fun JiraChangeLogItem.toDomain(jiraClient: JiraClient, entry: JiraChangeLogEntry) = ChangeLogItem(
    entry.created.toInstant(),
    field,
    fromString,
    toString,
    ::getUserGroups.partially1(jiraClient).partially1(entry.author.name)
)

fun JiraIssue.getChangeLogEntries(jiraClient: JiraClient) =
    changeLog.entries.flatMap { e -> e.items.map { i -> i.toDomain(jiraClient, e) } }
