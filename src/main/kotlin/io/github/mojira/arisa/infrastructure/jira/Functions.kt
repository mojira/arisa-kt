@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.infrastructure.jira

import arrow.core.Either
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.log
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

fun updateCHK(context: IssueUpdateContext, chkField: String) {
    context.hasTransitions = true
    context.transition.field(
        chkField,
        Instant
            .now()
            .with(ChronoField.NANO_OF_SECOND, 0)
            .with(ChronoField.MILLI_OF_SECOND, MILLI_FOR_FORMAT)
            .toString()
            .replace("Z", "-0000")
    )
}

fun updateConfirmation(context: IssueUpdateContext, confirmationField: String, value: String) {
    val jsonValue = JSONObject()
    jsonValue["value"] = value

    context.hasTransitions = true
    context.transition.field(confirmationField, jsonValue)
}

fun updateLinked(context: IssueUpdateContext, linkedField: String, value: Double) {
    context.hasTransitions = true
    context.transition.field(linkedField, value)
}

fun reopen(context: IssueUpdateContext) = runBlocking {
    context.hasTransitions = true
    context.transitionName = "Reopen Issue"
}

fun resolveAs(context: IssueUpdateContext, resolution: String) {
    val resolutionJson = JSONObject()
    resolutionJson["name"] = resolution

    context.hasTransitions = true
    context.transition.field(Field.RESOLUTION, resolutionJson)
    context.transitionName = "Resolve Issue"
}

fun updateSecurity(context: IssueUpdateContext, levelId: String) {
    context.hasTransitions = true
    context.transition.field(Field.SECURITY, Field.valueById(levelId))
}

fun removeAffectedVersion(context: IssueUpdateContext, version: Version) {
    context.hasUpdates = true
    context.update.fieldRemove("versions", version)
}

fun addAffectedVersionById(context: IssueUpdateContext, id: String) {
    context.hasUpdates = true
    context.update.fieldAdd("versions", Field.valueById(id))
}

fun addAffectedVersion(context: IssueUpdateContext, version: Version) {
    context.hasUpdates = true
    context.update.fieldAdd("versions", version)
}

fun updateDescription(context: IssueUpdateContext, description: String) {
    context.hasUpdates = true
    context.update.field(Field.DESCRIPTION, description)
}

@Suppress("TooGenericExceptionCaught")
fun applyUpdatesAndTransitions(context: IssueUpdateContext) {
    try {
        if (context.hasUpdates) {
            context.update.execute()
        }
        if (context.hasTransitions) {
            context.transition.execute(context.transitionName)
        }
    } catch (ex: Throwable) {
        log.error("Failed to apply updates and transitions", ex)
    }
}

fun deleteAttachment(jiraClient: JiraClient, attachment: Attachment): Either<Throwable, Unit> = runBlocking {
    Either.catch {
        jiraClient.restClient.delete(URI(attachment.self))
        Unit
    }
}

fun createComment(issue: Issue, comment: String) = runBlocking {
    Either.catch {
        issue.addComment(comment)
        Unit
    }
}

fun addRestrictedComment(issue: Issue, comment: String, restrictionLevel: String) = runBlocking {
    Either.catch {
        issue.addComment(comment, "group", restrictionLevel)
        Unit
    }
}

fun createLink(issue: Issue, linkType: String, linkKey: String) = runBlocking {
    Either.catch {
        issue.link(linkKey, linkType)
    }
}

fun deleteLink(link: IssueLink) = runBlocking {
    Either.catch {
        link.delete()
    }
}

fun updateCommentBody(comment: Comment, body: String) = runBlocking {
    Either.catch {
        comment.update(body)
        Unit
    }
}

fun restrictCommentToGroup(comment: Comment, group: String, body: String = comment.body) = runBlocking {
    Either.catch {
        comment.update(body, "group", group)
        Unit
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
