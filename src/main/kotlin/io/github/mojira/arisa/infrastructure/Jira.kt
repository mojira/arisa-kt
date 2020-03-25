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

typealias IssueId = String

fun updateCHK(issue: Issue, chkField: String): Either<Throwable, Unit> = runBlocking {
    Either.catch {
        issue
            .update()
            .field(chkField, Instant.now().toString())
            .execute()
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
    }
}

// The used library doesn't include visibility information in comments for whatever reason
fun isCommentRestrictedTo(jiraClient: JiraClient, comment: Comment, group: String) = runBlocking {
    Either.catch {
        val commentJson = jiraClient.restClient.get(URI(comment.self)) as JSONObject
        if (commentJson.contains("visibility")) {
            val visibility = commentJson.getJSONObject("visibility")
            visibility.get("type") == "group" && visibility.get("value") == group
        } else false
    }
}

fun resolveAsInvalid(issue: Issue) = runBlocking {
    Either.catch {
        issue.transition()
            .field(Field.RESOLUTION, "Invalid")
            .execute("Resolve Issue")
    }
}

fun updateCommentBody(jiraClient: JiraClient, comment: Comment, newValue: String) = runBlocking {
    Either.catch {
        jiraClient.restClient.put(URI(comment.self), JSONObject().element("body", newValue))
        Unit
    }
}

fun restrictCommentToGroup(jiraClient: JiraClient, comment: Comment, group: String) = runBlocking {
    Either.catch {
        jiraClient.restClient.put(
            URI(comment.self),
            JSONObject()
                .element("body", comment.body)
                .element("visibility",
                    JSONObject()
                        .element("type", "group")
                        .element("value", group)
                )
        )
        Unit
    }
}

fun updateAndRestrictCommentToGroup(jiraClient: JiraClient, comment: Comment, body: String, group: String) = runBlocking {
    Either.catch {
        jiraClient.restClient.put(
            URI(comment.self),
            JSONObject()
                .element("body", body)
                .element("visibility",
                    JSONObject()
                        .element("type", "group")
                        .element("value", group)
                )
        )
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
