package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import net.rcarz.jiraclient.Attachment
import net.rcarz.jiraclient.BasicCredentials
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Field
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.Version
import net.sf.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoField

typealias IssueId = String

fun updateCHK(issue: Issue, chkField: String): Either<Throwable, Unit> = runBlocking {
    Either.catch {
        issue
            .transition()
            .field(
                chkField,
                Instant
                    .now()
                    .with(ChronoField.NANO_OF_SECOND, 0)
                    .with(ChronoField.MILLI_OF_SECOND, 123)
                    .toString()
                    .replace("Z", "-0000")
            )
            .execute("Update Issue")
    }
}

fun deleteAttachment(jiraClient: JiraClient, attachment: Attachment): Either<Throwable, Unit> = runBlocking {
    Either.catch {
        jiraClient.restClient.delete(URI(attachment.self))
        Unit
    }
}

fun connectToJira(username: String, password: String, url: String) =
    JiraClient(url, BasicCredentials(username, password))

fun reopenIssue(issue: Issue) = runBlocking {
    Either.catch {
        issue.transition().execute("Reopen Issue")
    }
}

fun addComment(issue: Issue, comment: String) = runBlocking {
    Either.catch {
        issue.addComment(comment)
        Unit
    }
}

fun resolveAs(issue: Issue, resolution: String) = runBlocking {
    Either.catch {
        val resolutionJson = JSONObject()
        resolutionJson["name"] = resolution

        issue.transition()
            .field(Field.RESOLUTION, resolutionJson)
            .execute("Resolve Issue")
    }
}

fun link(issue: Issue, linkType: String, linkKey: String) = runBlocking {
    Either.catch {
        issue.link(linkKey, linkType)
    }
}

fun updateCommentBody(comment: Comment, body: String) = runBlocking {
    Either.catch {
        comment.update(body)
        Unit
    }
}

fun restrictCommentToGroup(comment: Comment, group: String, body: String? = comment.body) = runBlocking {
    Either.catch {
        comment.update(body, "group", group)
        Unit
    }
}

fun removeAffectedVersion(issue: Issue, version: Version) = runBlocking {
    Either.catch {
        issue
            .update()
            .fieldRemove("versions", version)
            .execute()
    }
}

fun addAffectedVersion(issue: Issue, version: Version) = runBlocking {
    Either.catch {
        issue
            .update()
            .fieldAdd("versions", version)
            .execute()
    }
}

fun updateSecurity(issue: Issue, levelId: String) = runBlocking {
    Either.catch {
        val securityJson = JSONObject()
        securityJson["id"] = levelId

        issue
            .update()
            .field("security", securityJson)
            .execute()
    }
}
