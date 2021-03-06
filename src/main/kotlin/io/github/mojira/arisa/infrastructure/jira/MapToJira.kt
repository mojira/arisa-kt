package io.github.mojira.arisa.infrastructure.jira

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.infrastructure.config.Arisa
import net.rcarz.jiraclient.Field
import net.sf.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoField

fun JiraIssue.mapToJira(config: Config): MapToJira = MapToJira(this, config)

class MapToJira(val issue: JiraIssue, val config: Config) {
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
        // TODO prevent comments being added in the next run again
        comments.forEach {
            issue.addComment(it.body, it.visibilityType, it.visibilityValue)
        }
    }

    fun editComments(comments: List<Comment>) {
        comments.forEach { comment ->
            issue.comments
                .firstOrNull { it.id == comment.id }
                ?.update(comment.body, comment.visibilityType, comment.visibilityValue)
        }
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