package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.modules.*
import io.github.mojira.arisa.Modules.execute
import net.rcarz.jiraclient.BasicCredentials
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("Arisa")

val config = Config { addSpec(Arisa) }
    .from.yaml.watchFile("arisa.yml")
    .from.env()
    .from.systemProperties()

object Arisa : ConfigSpec() {
    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
    }
    object Issues: ConfigSpec() {
        val projects by required<String>()
        val url by optional("https://bugs.mojang.com/")
        val checkInterval by optional(10L)
    }
    object CustomFields: ConfigSpec() {
        val chkField by optional("customfield_10701")
        val confirmationField by optional("customfield_10500")
    }

    object Modules: ConfigSpec() {
        object Attachment: ConfigSpec() {
            val extensionBlacklist by optional("jar,exe,com,bat,msi,run,lnk,dmg")
        }
    }
}

object Modules {
    val jiraClient by lazy {
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password]
        )
    }
    val attachmentModule by lazy { AttachmentModule(
        jiraClient,
        config
    ) }
    val chkModule by lazy { CHKModule(
        jiraClient,
        config
    ) }

    fun connectToJira(username: String, password: String): JiraClient {
        val creds = BasicCredentials(username, password)
        return JiraClient(config[Arisa.Issues.url], creds)
    }

    fun execute(issue: Issue) = listOf(
        attachmentModule(AttachmentModuleRequest(issue.attachments)),
        chkModule(
            CHKModuleRequest(
                issue.key,
                issue.getField(config[Arisa.CustomFields.chkField]) as? String?,
                issue.getField(config[Arisa.CustomFields.confirmationField]) as? String?
            )
        )
    )
}

fun main() {
    while (true) {
        val resolutions = listOf("Unresolved", "\"Awaiting Response\"").joinToString(", ")
        val projects = config[Arisa.Issues.projects]
        val jql = "project in ($projects) AND resolution in ($resolutions) AND updated >= -5m"

        try {
            Modules.jiraClient
                .searchIssues(jql)
                .issues
                .map(::execute)
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
