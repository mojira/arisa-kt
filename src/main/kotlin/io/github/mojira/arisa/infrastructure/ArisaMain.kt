package io.github.mojira.arisa.infrastructure

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.application.*
import io.github.mojira.arisa.domain.model.Attachment
import io.github.mojira.arisa.infrastructure.service.jira.JiraDeleteAttachmentService
import io.github.mojira.arisa.infrastructure.service.jira.JiraUpdateCHKService
import net.rcarz.jiraclient.BasicCredentials
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import java.util.concurrent.TimeUnit

// Custom fields
const val CHKFIELD = "customfield_10701"
const val CONFIRMATIONFIELD = "customfield_10500"


val config = Config { addSpec(ArisaSpec) }
    .from.yaml.watchFile("arisa.yml")
    .from.env()
    .from.systemProperties()

// Services
val jiraClient by lazy { connectToJira(config[ArisaSpec.username], config[ArisaSpec.password]) }
val jiraDeleteAttachmentService by lazy { JiraDeleteAttachmentService(jiraClient) }
val updateCHKService by lazy { JiraUpdateCHKService(jiraClient) }

// Modules
val attachmentModule by lazy { AttachmentModule(jiraDeleteAttachmentService) }
val chkModule by lazy { CHKModule(updateCHKService) }

fun main() {
    ArisaMain().main()
}

class ArisaMain {
    fun main() {
        while (true) {
            val jql =
                "project in (${config[ArisaSpec.projects]}) AND resolution in (Unresolved, \"Awaiting Response\") AND updated >= -5m"

            val issues = try {
                jiraClient.searchIssues(jql)
            } catch (e: Exception) {
                log.error("Error retrieving issues", e)
                continue
            }

            if (issues.total == 0) {
                log.info("No issues to process")
                continue
            }

            issues
                .issues
                .map(::executeModules)
                .toMap()
                .forEach(::throwIfError)

            TimeUnit.SECONDS.sleep(config[ArisaSpec.checkInterval])
        }

    }

    private fun executeModules(issue: Issue) = issue to listOf(
        attachmentModule(AttachmentModuleRequest(issue.attachments.map { Attachment(it.id, it.contentUrl) })),
        chkModule(
            CHKModuleRequest(
                issue.key,
                issue.getField(CHKFIELD) as? String?,
                issue.getField(CONFIRMATIONFIELD) as? String?
            )
        )
    )

    private fun throwIfError(map: Map.Entry<Issue, List<ModuleResponse>>) = map
        .value
        .filterIsInstance<FailedModuleResponse>()
        .forEach {
            it.exceptions.forEach {
                log.error("Error executing module for ticket ${map.key}", it)
            }
        }
}

fun connectToJira(username: String, password: String): JiraClient {
    val creds = BasicCredentials(username, password)
    return JiraClient("https://bugs.mojang.com/", creds)
}

object ArisaSpec : ConfigSpec() {
    val username by required<String>()
    val password by required<String>()
    val projects by required<String>()
    val checkInterval by optional(10L)
}