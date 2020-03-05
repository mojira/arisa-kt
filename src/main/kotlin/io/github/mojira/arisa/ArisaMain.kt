package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.right
import arrow.syntax.function.partially1
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.addComment
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.connectToJira
import io.github.mojira.arisa.infrastructure.deleteAttachment
import io.github.mojira.arisa.infrastructure.reopenIssue
import io.github.mojira.arisa.infrastructure.resolveAsInvalid
import io.github.mojira.arisa.infrastructure.updateCHK
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.AttachmentModuleRequest
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CHKModuleRequest
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.PiracyModuleRequest
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReopenAwaitingModuleRequest
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("Arisa")

fun main() {
    val config = Config { addSpec(Arisa) }
        .from.yaml.watchFile("arisa.yml")
        .from.env()
        .from.systemProperties()

    val jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )

    val executeModules = initModules(config, jiraClient)
    while (true) {
        val resolutions = listOf("Unresolved", "\"Awaiting Response\"").joinToString(", ")
        val projects = config[Arisa.Issues.projects]
        val jql = "project in ($projects) AND resolution in ($resolutions) AND updated >= -5m"

        try {
            jiraClient
                .searchIssues(jql)
                .issues
                .flatMap(executeModules)
                .filter(Either<ModuleError, ModuleResponse>::isLeft)
                .filterIsInstance<FailedModuleResponse>()
                .flatMap { it.exceptions }
                .forEach { log.error("Error executing module", it) }
        } catch (e: Exception) {
            log.error("Failed to get issues", e)
            continue
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkInterval])
    }
}

fun initModules(config: Config, jiraClient: JiraClient): (Issue) -> List<Either<ModuleError, ModuleResponse>> {

    return { issue: Issue ->
        val attachmentModule = AttachmentModule(
            if (!config[Arisa.shadow]) ::deleteAttachment.partially1(jiraClient) else { a -> Unit.right() },
            config[Arisa.Modules.Attachment.extensionBlacklist].split(",")
        )
        val chkModule = CHKModule(
            if (!config[Arisa.shadow]) ::updateCHK.partially1(issue).partially1(config[Arisa.CustomFields.chkField]) else ({ Unit.right() })
        )
        val reopenAwaitingModule = ReopenAwaitingModule(
            if (!config[Arisa.shadow]) ::reopenIssue.partially1(issue) else ({ Unit.right() })
        )
        val piracyModule = PiracyModule(
            if (!config[Arisa.shadow]) ::resolveAsInvalid.partially1(issue) else ({ Unit.right() }),
            if (!config[Arisa.shadow]) ::addComment.partially1(issue).partially1(config[Arisa.Modules.Piracy.piracyMessage]) else ({ Unit.right() }),
            config[Arisa.Modules.Piracy.piracySignatures].split(",")
        )

        listOf(
            attachmentModule(AttachmentModuleRequest(issue.attachments)),
            chkModule(
                CHKModuleRequest(
                    issue.key,
                    issue.getField(config[Arisa.CustomFields.chkField]) as? String?,
                    issue.getField(config[Arisa.CustomFields.confirmationField]) as? String?
                )
            ),
            reopenAwaitingModule(
                ReopenAwaitingModuleRequest(
                    issue.resolution,
                    issue.getField("created") as Date,
                    issue.getField("updated") as Date,
                    issue.comments
                )
            ),
            piracyModule(
                PiracyModuleRequest(issue.getField("environment") as String?, issue.summary, issue.description)
            )
        )
    }
}
