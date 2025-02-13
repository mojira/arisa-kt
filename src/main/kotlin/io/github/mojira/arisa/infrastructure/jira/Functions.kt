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
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Field
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.IssueLink
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException
import net.rcarz.jiraclient.Resource
import net.rcarz.jiraclient.RestException
import net.rcarz.jiraclient.TokenCredentials
import net.rcarz.jiraclient.User
import net.rcarz.jiraclient.Version
import net.sf.json.JSONObject
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoField

fun connectToJira(username: String, password: String, url: String): JiraClient {
    val credentials = TokenCredentials(username, password)
    val jiraClient = JiraClient(url, credentials)
    val actualUsername = jiraClient.getCurrentUser().name

    // Jira seems to allow login with different capitalization, however for checks such
    // as whether an action was performed by Arisa itself, Arisa needs to know the correctly
    // capitalized username
    if (username != actualUsername) {
        throw IncorrectlyCapitalizedUsernameException(actualUsername)
    }
    return jiraClient
}

class IncorrectlyCapitalizedUsernameException(expectedUsername: String) :
    Exception("Username uses incorrect capitalization; expected: $expectedUsername")

private fun JiraClient.getCurrentUser(): User {
    // https://docs.atlassian.com/software/jira/docs/api/REST/7.6.1/#api/2/myself-getUser
    return object : User(
        restClient,
        restClient.get(Resource.getBaseUri() + "myself") as JSONObject
    ) {}
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
                "[]",
                amount
            )
        } catch (e: JiraException) {
            log.error("Error while retrieving filter results", e)
            throw e
        }

        searchResult.issues.mapNotNull { it.key }
    }
}

fun getIssue(jiraClient: JiraClient, key: String) = runBlocking {
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

private fun applyFluentUpdate(edit: Issue.FluentUpdate) = runBlocking {
    Either.catch {
        try {
            edit.execute()
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

private fun applyFluentTransition(update: Issue.FluentTransition, transitionName: String) = runBlocking {
    Either.catch {
        update.execute(transitionName)
    }
}

fun openAttachmentStream(jiraClient: JiraClient, attachment: Attachment): InputStream {
    val httpClient = jiraClient.restClient.httpClient
    val request = HttpGet(attachment.contentUrl)

    return runBlocking(Dispatchers.IO) {
        val response = httpClient.execute(request)
        val statusCode = response.statusLine.statusCode
        if (statusCode != HttpStatus.SC_OK) {
            throw IOException("Request for attachment ${attachment.id} content failed with status code $statusCode")
        }
        response.entity.content
    }
}

fun deleteAttachment(context: Lazy<IssueUpdateContext>, attachment: Attachment) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                withContext(Dispatchers.IO) {
                    try {
                        context.value.jiraClient.restClient.delete(URI(attachment.self))
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
                        context.value.jiraIssue.addAttachment(file)
                    } catch (e: RestException) {
                        if (e.httpStatusCode == HttpStatus.SC_NOT_FOUND ||
                            e.httpStatusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR
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
                    is Either.Right -> context.value.jiraIssue.addComment(comment)
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
                    is Either.Right -> context.value.jiraIssue.addComment(comment, "group", restrictionLevel)
                }

                Unit
            }
        }
    }
}

fun deleteComment(context: Lazy<IssueUpdateContext>, comment: Comment) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                withContext(Dispatchers.IO) {
                    try {
                        context.value.jiraClient.restClient.delete(URI(comment.self))
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
                Unit
            }
        }
    }
}

fun createLink(
    context: Lazy<IssueUpdateContext>,
    getContext: (key: String) -> Lazy<IssueUpdateContext>,
    linkType: String,
    linkKey: String,
    outwards: Boolean
) {
    if (outwards) {
        context.value.otherOperations.add {
            runBlocking {
                Either.catch {
                    context.value.jiraIssue.link(linkKey, linkType)
                }
            }
        }
    } else {
        runBlocking {
            val either = Either.catch {
                val key = context.value.jiraIssue.key
                createLink(getContext(linkKey), getContext, linkType, key, true)
            }
            if (either.isLeft()) {
                context.value.otherOperations.add { either }
            }
        }
    }
}

fun deleteLink(context: Lazy<IssueUpdateContext>, link: IssueLink) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                link.delete()
            }
        }
    }
}

fun updateCommentBody(context: Lazy<IssueUpdateContext>, comment: Comment, body: String) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                tryWithWarn(comment) {
                    comment.update(body)
                }
            }
        }
    }
}

fun restrictCommentToGroup(
    context: Lazy<IssueUpdateContext>,
    comment: Comment,
    group: String,
    body: String = comment.body
) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                tryWithWarn(comment) {
                    comment.update(body, "group", group)
                }
            }
        }
    }
}

fun tryWithWarn(comment: Comment, func: () -> Unit) {
    try {
        func()
    } catch (e: JiraException) {
        val cause = e.cause
        if (cause is RestException && (
            cause.httpStatusCode == HttpStatus.SC_NOT_FOUND ||
                cause.httpStatusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR
            )
        ) {
            log.warn("Tried to update comment ${comment.url} but it was deleted")
        } else {
            throw e
        }
    }
}

// not included in used library
fun getGroups(jiraClient: JiraClient, username: String) = runBlocking {
    Either.catch {
        withContext(Dispatchers.IO) {
            // Mojira does not seem to provide any accountIds, hence the endpoint GET /user/groups cannot be used.
            (
                jiraClient.restClient.get(
                    User.getBaseUri() + "user/",
                    mapOf(Pair("username", username), Pair("expand", "groups"))
                ) as JSONObject
                )
                .getJSONObject("groups")
                .getJSONArray("items")
                .map { (it as JSONObject)["name"] as String }
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
