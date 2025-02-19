@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.infrastructure.CommentCache
import io.github.mojira.arisa.log
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.tryRunAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.rcarz.jiraclient.Attachment
import io.github.mojira.arisa.infrastructure.apiclient.JiraClient as MojiraClient
import io.github.mojira.arisa.infrastructure.apiclient.models.AttachmentBean as MojiraAttachment
import io.github.mojira.arisa.domain.cloud.CloudAttachment
import net.rcarz.jiraclient.Comment
import io.github.mojira.arisa.infrastructure.apiclient.models.Comment as MojiraComment
import net.rcarz.jiraclient.Field
import net.rcarz.jiraclient.IssueLink
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueLink as MojiraIssueLink
import net.rcarz.jiraclient.JiraException
import io.github.mojira.arisa.infrastructure.apiclient.exceptions.JiraClientException
import net.rcarz.jiraclient.RestException
import io.github.mojira.arisa.infrastructure.apiclient.exceptions.ClientErrorException
import net.rcarz.jiraclient.Version
import net.sf.json.JSONObject
import org.apache.http.HttpStatus
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoField
import io.github.mojira.arisa.infrastructure.apiclient.JiraClient
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueBean as MojiraIssue
import io.github.mojira.arisa.infrastructure.apiclient.models.Visibility
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.UpdateCommentBody
import io.github.mojira.arisa.jiraClient

fun connectToJira(email: String, apiToken: String, url: String): JiraClient {
    return JiraClient(url, email, apiToken)
}

/**
 * Get a list of tickets matching a JQL query.
 * TODO: Actually return the tickets themselves instead of only ticket IDs.
 *
 * @param amount How many tickets should be returned at most.
 *
 * @return a list of strings indicating ticket IDs that are contained in the given jql filter.
 */
@Suppress("ForbiddenComment")
fun getIssuesFromJql(jiraClient: JiraClient, jql: String, amount: Int) = runBlocking {
    Either.catch {
        val searchResult = try {
            jiraClient.searchIssues(
                jql,
                listOf("[]"),
                maxResults = amount
            )
        } catch (e: JiraException) {
            log.error("Error while retrieving filter results", e)
            throw e
        }

        searchResult.issues.mapNotNull { it.key }
    }
}

fun getIssue(jiraClient: MojiraClient, key: String) = runBlocking {
    Either.catch {
        jiraClient.getIssue(key, "*all", "changelog")
    }
}

const val MILLI_FOR_FORMAT = 123L

fun updateCHK(context: Lazy<IssueUpdateContext>, chkField: String) {
    context.value.hasUpdates = true
    context.value.update.field(
        chkField,
        Instant
            .now()
            .with(ChronoField.NANO_OF_SECOND, 0)
            .with(ChronoField.MILLI_OF_SECOND, MILLI_FOR_FORMAT)
            .toString()
            .replace("Z", "-0000")
    )
}

fun updateConfirmation(context: Lazy<IssueUpdateContext>, confirmationField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["value"] = value

    context.value.hasUpdates = true
    context.value.update.field(confirmationField, jsonValue)
}

fun updatePriority(context: Lazy<IssueUpdateContext>, priorityField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["id"] = value

    context.value.hasUpdates = true
    context.value.update.field(priorityField, jsonValue)
}

fun updateDungeonsPlatform(context: Lazy<IssueUpdateContext>, dungeonsPlatformField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["value"] = value

    context.value.hasEdits = true
    context.value.edit.field(dungeonsPlatformField, jsonValue)
}

fun updateLegendsPlatform(context: Lazy<IssueUpdateContext>, legendsPlatformField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["value"] = value

    context.value.hasEdits = true
    context.value.edit.field(legendsPlatformField, jsonValue)
}

fun updatePlatform(context: Lazy<IssueUpdateContext>, platformField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["value"] = value

    context.value.hasEdits = true
    context.value.edit.field(platformField, jsonValue)
}

fun updateLinked(context: Lazy<IssueUpdateContext>, linkedField: String, value: Double) {
    context.value.hasUpdates = true
    context.value.update.field(linkedField, value)
}

fun reopen(context: Lazy<IssueUpdateContext>) {
    context.value.transitionName = "Reopen Issue"
}

fun resolveAs(context: Lazy<IssueUpdateContext>, resolution: String) {
    val resolutionJson = JSONObject()
    resolutionJson["name"] = resolution

    context.value.resolve.field(Field.RESOLUTION, resolutionJson)
    context.value.transitionName = "Resolve Issue"
}

fun updateSecurity(context: Lazy<IssueUpdateContext>, levelId: String) {
    context.value.hasEdits = true
    context.value.edit.field(Field.SECURITY, Field.valueById(levelId))
}

fun removeAffectedVersion(context: Lazy<IssueUpdateContext>, version: Version) {
    context.value.hasEdits = true
    context.value.edit.fieldRemove("versions", version)
}

fun addAffectedVersionById(context: Lazy<IssueUpdateContext>, id: String) {
    context.value.hasEdits = true
    context.value.edit.fieldAdd("versions", Field.valueById(id))
}

fun removeAffectedVersionById(context: Lazy<IssueUpdateContext>, id: String) {
    context.value.hasEdits = true
    context.value.edit.fieldRemove("versions", Field.valueById(id))
}

fun addAffectedVersion(context: Lazy<IssueUpdateContext>, version: Version) {
    context.value.hasEdits = true
    context.value.edit.fieldAdd("versions", version)
}

fun updateDescription(context: Lazy<IssueUpdateContext>, description: String) {
    context.value.hasEdits = true
    context.value.edit.field(Field.DESCRIPTION, description)
}

fun applyIssueChanges(context: IssueUpdateContext): Either<FailedModuleResponse, ModuleResponse> {
    val functions = context.otherOperations.toMutableList()
    if (context.hasEdits) {
        functions.add(0, ::applyFluentUpdate.partially1(context.edit))
    }
    if (context.hasUpdates) {
        functions.add(
            0,
            ::applyFluentTransition.partially1(context.update).partially1("Update Issue")
        )
    }
    if (context.transitionName != null) {
        functions.add(
            0,
            ::applyFluentTransition.partially1(context.resolve).partially1(context.transitionName!!)
        )
    }
    return tryRunAll(functions, context)
}

private fun applyFluentUpdate(edit: MojiraIssue.FluentUpdate) = runBlocking {
    Either.catch {
        try {
            val (requestBody, updatedIssue) = edit.execute()
            jiraClient.editIssue(updatedIssue.id, requestBody)
        } catch (e: JiraException) {
            val cause = e.cause
            if (cause is RestException && (
                    cause.httpStatusCode == HttpStatus.SC_NOT_FOUND ||
                        cause.httpStatusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
            ) {
                log.warn("Failed to execute fluent update due to ${cause.httpStatusCode}")
            } else {
                throw e
            }
        }
    }
}

private fun applyFluentTransition(update: MojiraIssue.FluentTransition, transitionName: String) = runBlocking {
    Either.catch {
        update.execute(transitionName)
    }
}

fun openAttachmentStream(jiraClient: JiraClient, attachment: MojiraAttachment): InputStream {
    return jiraClient.openAttachmentStream(attachment.id)
}

fun deleteAttachment(context: Lazy<IssueUpdateContext>, attachment: MojiraAttachment) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                withContext(Dispatchers.IO) {
                    try {
                        context.value.jiraClient.deleteAttachment(attachment.id)
                    } catch (e: RestException) {
                        if (e.httpStatusCode == HttpStatus.SC_NOT_FOUND ||
                            e.httpStatusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR
                        ) {
                            log.warn("Tried to delete ${attachment.id} when it was already deleted")
                        } else {
                            throw e
                        }
                    }
                }
                Unit
            }
        }
    }
}

fun addAttachmentFile(context: Lazy<IssueUpdateContext>, file: File, cleanupCallback: () -> Unit) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                withContext(Dispatchers.IO) {
                    try {
                        val issueId = context.value.jiraIssue.id
                        context.value.jiraClient.addAttachment(issueId, file)
                        Unit
                    } catch (e: ClientErrorException) {
                        if (e.code == HttpStatus.SC_NOT_FOUND ||
                            e.code >= HttpStatus.SC_INTERNAL_SERVER_ERROR
                        ) {
                            log.warn("Couldn't upload ${file.name}")
                        } else {
                            throw e
                        }
                    } finally {
                        cleanupCallback()
                    }
                }
            }
        }
    }
}

fun createComment(
    context: Lazy<IssueUpdateContext>,
    comment: String
) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                val key = context.value.jiraIssue.key

                when (val checkResult = CommentCache.check(key, comment)) {
                    is Either.Left -> log.error(checkResult.a.message)
                    is Either.Right -> {
                        val issueId = context.value.jiraIssue.id
                        context.value.jiraClient.addComment(issueId, comment)
                    }
                }

                Unit
            }
        }
    }
}

fun addRestrictedComment(
    context: Lazy<IssueUpdateContext>,
    comment: String,
    restrictionLevel: String
) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                val key = context.value.jiraIssue.key

                when (val checkResult = CommentCache.check(key, comment)) {
                    is Either.Left -> log.error(checkResult.a.message)
                    is Either.Right -> {
                        val issueId = context.value.jiraIssue.id
                        val visibility = Visibility(value = restrictionLevel)
                        context.value.jiraClient.addRestrictedComment(issueId, comment, visibility)
                    }
                }

                Unit
            }
        }
    }
}

fun deleteComment(context: Lazy<IssueUpdateContext>, comment: MojiraComment) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                withContext(Dispatchers.IO) {
                    try {
                        val issueId = context.value.jiraIssue.id
                        context.value.jiraClient.deleteComment(issueId, comment.id)
                    } catch (e: RestException) {
                        if (e.httpStatusCode == HttpStatus.SC_NOT_FOUND ||
                            e.httpStatusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR
                        ) {
                            log.warn("Tried to delete ${comment.id} when it was already deleted")
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
    }
}

fun createLink(
    context: Lazy<IssueUpdateContext>,
    getContext: (key: String) -> Lazy<IssueUpdateContext>,
    linkType: String,
    linkIssueId: String,
    outwards: Boolean
) {
    if (outwards) {
        context.value.otherOperations.add {
            runBlocking {
                Either.catch {
                    context.value.jiraIssue.link(jiraClient, linkIssueId, linkType)
                }
            }
        }
    } else {
        runBlocking {
            val either = Either.catch {
                val key = context.value.jiraIssue.key
                createLink(getContext(linkIssueId), getContext, linkType, key, true)
            }
            if (either.isLeft()) {
                context.value.otherOperations.add { either }
            }
        }
    }
}

fun deleteLink(context: Lazy<IssueUpdateContext>, link: MojiraIssueLink) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                jiraClient.deleteIssueLink(link.id!!)
            }
        }
    }
}

fun updateCommentBody(context: Lazy<IssueUpdateContext>, comment: MojiraComment, body: String) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                tryWithWarn(comment) {
                    val issueId = context.value.jiraIssue.id
                    context.value.jiraClient.updateComment(
                        issueId,
                        comment.id,
                        UpdateCommentBody(body)
                    )
                }
            }
        }
    }
}

fun restrictCommentToGroup(
    context: Lazy<IssueUpdateContext>,
    comment: MojiraComment,
    group: String,
    body: String = comment.body
) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                tryWithWarn(comment) {
                    val issueId = context.value.jiraIssue.id
                    val payload = UpdateCommentBody(
                        body,
                        visibility = Visibility(
                            identifier = group,
                            type = Visibility.Type.Group,
                            value = group
                        )
                    )
                    context.value.jiraClient.updateComment(
                        issueId,
                        comment.id,
                        payload
                    )
                }
            }
        }
    }
}

fun tryWithWarn(comment: MojiraComment, func: () -> Unit) {
    try {
        func()
    } catch (e: JiraException) {
        val cause = e.cause
        if (cause is ClientErrorException && (
                cause.code == HttpStatus.SC_NOT_FOUND ||
                    cause.code >= HttpStatus.SC_INTERNAL_SERVER_ERROR
                )
        ) {
            log.warn("Tried to update comment ${comment.self} but it was deleted")
        } else {
            throw e
        }
    }
}

// not included in used library
fun getGroups(jiraClient: JiraClient, accountId: String) = runBlocking {
    Either.catch {
        withContext(Dispatchers.IO) {
            jiraClient.getUserGroups(accountId).map { group -> group.name }
        }
    }
}

fun markAsFixedWithSpecificVersion(context: Lazy<IssueUpdateContext>, fixVersionName: String) {
    context.value.resolve.field(Field.FIX_VERSIONS, listOf(mapOf("name" to fixVersionName)))
    context.value.transitionName = "Resolve Issue"
}

fun changeReporter(context: Lazy<IssueUpdateContext>, reporter: String) {
    context.value.edit.field(Field.REPORTER, reporter)
}

// Allows some basic Jira formatting characters to be used by helper message arguments;
// when used by malicious user they should at most cause text formatting errors
private val sanitizationRegex = Regex("[^a-zA-Z0-9\\-+_#*?.,; ]")
fun sanitizeCommentArg(arg: String): String {
    return arg.replace(sanitizationRegex, "?")
}
