@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.tryRunAll
import kotlinx.coroutines.runBlocking
import net.rcarz.jiraclient.Attachment
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Field
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.IssueLink
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.TokenCredentials
import net.rcarz.jiraclient.User
import net.rcarz.jiraclient.Version
import net.sf.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoField

fun connectToJira(username: String, password: String, url: String) =
    JiraClient(url, TokenCredentials(username, password))

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
    context.value.hasUpdates = true
    context.value.update.field(Field.SECURITY, Field.valueById(levelId))
}

fun removeAffectedVersion(context: Lazy<IssueUpdateContext>, version: Version) {
    context.value.hasEdits = true
    context.value.edit.fieldRemove("versions", version)
}

fun addAffectedVersionById(context: Lazy<IssueUpdateContext>, id: String) {
    context.value.hasEdits = true
    context.value.edit.fieldAdd("versions", Field.valueById(id))
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
        functions.add(::applyFluentUpdate.partially1(context.edit))
    }
    if (context.hasUpdates) {
        functions.add(
            ::applyFluentTransition.partially1(context.update).partially1("Update Issue"))
    }
    if (context.transitionName != null) {
        functions.add(
            ::applyFluentTransition.partially1(context.resolve).partially1(context.transitionName!!)
        )
    }
    return tryRunAll(functions)
}

private fun applyFluentUpdate(edit: Issue.FluentUpdate) = runBlocking {
    Either.catch {
        edit.execute()
    }
}

private fun applyFluentTransition(update: Issue.FluentTransition, transitionName: String) = runBlocking {
    Either.catch {
        update.execute(transitionName)
    }
}

fun deleteAttachment(context: Lazy<IssueUpdateContext>, attachment: Attachment) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                context.value.jiraClient.restClient.delete(URI(attachment.self))
                Unit
            }
        }
    }
}

fun createComment(context: Lazy<IssueUpdateContext>, comment: String) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                context.value.jiraIssue.addComment(comment)
                Unit
            }
        }
    }
}

fun addRestrictedComment(context: Lazy<IssueUpdateContext>, comment: String, restrictionLevel: String) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                context.value.jiraIssue.addComment(comment, "group", restrictionLevel)
                Unit
            }
        }
    }
}

fun createLink(context: Lazy<IssueUpdateContext>, linkType: String, linkKey: String) {
    context.value.otherOperations.add {
        runBlocking {
            Either.catch {
                context.value.jiraIssue.link(linkKey, linkType)
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
                comment.update(body)
                Unit
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
                comment.update(body, "group", group)
                Unit
            }
        }
    }
}

// not included in used library
fun getGroups(jiraClient: JiraClient, username: String) = runBlocking {
    Either.catch {
        // Mojira does not seem to provide any accountIds, hence the endpoint GET /user/groups cannot be used.
        (jiraClient.restClient.get(
            User.getBaseUri() + "user/",
            mapOf(Pair("username", username), Pair("expand", "groups"))
        ) as JSONObject)
            .getJSONObject("groups")
            .getJSONArray("items")
            .map { (it as JSONObject)["name"] as String }
    }
}
