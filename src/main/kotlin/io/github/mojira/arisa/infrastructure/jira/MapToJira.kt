package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.service.CommentCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.jiraClient
import io.github.mojira.arisa.log
import net.rcarz.jiraclient.Field
import net.sf.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoField

class MapToJira(val config: Config, val commentCache: CommentCache) {

    fun startMap(issue: JiraIssue) = Builder(issue)

    inner class Builder(val issue: JiraIssue) {
        val edit = issue.update()
        var hasEdit = false
        val update = issue.transition()
        var hasUpdate = false
        val transition = issue.transition()
        var transitionName: String? = null

        fun updateSecurityLevel(level: String) {
            edit.field(Field.SECURITY, Field.valueById(level))
            hasEdit = true
        }

        fun resolve(resolution: String) {
            val resolutionJson = JSONObject()
            resolutionJson["name"] = resolution

            transition.field(Field.RESOLUTION, resolutionJson)
            transitionName = "Resolve Issue"
        }

        fun updateDescription(updatedDescription: String) {
            edit.field(Field.DESCRIPTION, updatedDescription)
            hasEdit = true
        }

        fun updateChk() {
            hasUpdate = true
            update.field(
                config[Arisa.CustomFields.chkField],
                Instant.now()
                    .with(ChronoField.NANO_OF_SECOND, 0)
                    .with(ChronoField.MILLI_OF_SECOND, 123L)
                    .toString()
                    .replace("Z", "-0000")
            )
        }

        fun updateLinked(linked: Double) {
            hasUpdate = true
            update.field(config[Arisa.CustomFields.linked], linked)
        }

        fun addComments(comments: List<Comment>) {
            comments.forEach {
                if (!commentCache.hasBeenPostedBefore(issue.key, it.body)) {
                    issue.addComment(it.body, it.visibilityType, it.visibilityValue)
                } else {
                    log.error("Tried to add a comment that has been posted before. Key: ${issue.key}, Comment: ${it.body}")
                }
                commentCache.addPost(issue.key, it.body)
            }
        }

        fun editComments(comments: List<Comment>) {
            comments.forEach { comment ->
                issue.comments
                    .firstOrNull { it.id == comment.id }
                    ?.update(comment.body, comment.visibilityType, comment.visibilityValue)
            }
        }

        fun addOutwardsLink(link: Link) {
            issue.link(link.issue.key, link.type)
        }

        fun addInwardsLink(link: Link, otherIssue: JiraIssue) {
            otherIssue.link(link.issue.key, link.type)
        }

        fun removeLink(link: Link) {
            issue.issueLinks.firstOrNull { it.id == link.id }?.delete()
        }

        fun removeAttachments(removedAttachments: MutableList<Attachment>) {
            val attachmentIds = removedAttachments.map { it.id }
            issue.attachments.filter { removedAttachments.any { attachmentIds.contains(it.id) } }.forEach { deleteAttachment(it.self) }
        }

        private fun deleteAttachment(self: String) {
            jiraClient.restClient.delete(URI(self))
        }

        fun execute() {
            if (hasEdit) {
                edit.execute()
            }
            if (hasUpdate) {
                update.execute("Update Issue")
            }
            if (transitionName != null) {
                transition.execute(transitionName!!)
            }
        }
    }
}