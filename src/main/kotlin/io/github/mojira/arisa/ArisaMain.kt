package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.addAffectedVersion
import io.github.mojira.arisa.infrastructure.addComment
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.connectToJira
import io.github.mojira.arisa.infrastructure.deleteAttachment
import io.github.mojira.arisa.infrastructure.link
import io.github.mojira.arisa.infrastructure.removeAffectedVersion
import io.github.mojira.arisa.infrastructure.reopenIssue
import io.github.mojira.arisa.infrastructure.resolveAs
import io.github.mojira.arisa.infrastructure.restrictCommentToGroup
import io.github.mojira.arisa.infrastructure.updateCHK
import io.github.mojira.arisa.infrastructure.updateCommentBody
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.AttachmentModuleRequest
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CHKModuleRequest
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.CrashModuleRequest
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.EmptyModuleRequest
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.FutureVersionModuleRequest
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.PiracyModuleRequest
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModuleRequest
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModuleRequest
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReopenAwaitingModuleRequest
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("Arisa")

fun main() {
    val config = Config { addSpec(Arisa) }
        .from.json.watchFile("arisa.json")
        .from.env()
        .from.systemProperties()

    val jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )

    log.info("Connected to jira")

    val executeModules = initModules(config, jiraClient)
    while (true) {
        val resolutions = listOf("Unresolved", "\"Awaiting Response\"").joinToString(", ")
        val projects = config[Arisa.Issues.projects].joinToString(", ")
        val jql = "project in ($projects) AND resolution in ($resolutions) AND updated >= -5m"

        try {
            jiraClient
                .searchIssues(jql)
                .issues
                .map { it.key to executeModules(it) }
                .forEach { (issue, responses) ->
                    responses.forEach { (module, response) ->
                        when (response) {
                            is Either.Right -> log.info("[RESPONSE] [$issue] [$module] Successful")
                            is Either.Left -> {
                                when (response.a) {
                                    // is OperationNotNeededModuleResponse -> log.info("[RESPONSE] [$issue] [$module] Operation not needed")
                                    is FailedModuleResponse -> for (exception in (response.a as FailedModuleResponse).exceptions) {
                                        log.error("[RESPONSE] [$issue] [$module] Failed", exception)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            log.error("Failed to get issues", e)
            continue
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkInterval])
    }
}

fun initModules(config: Config, jiraClient: JiraClient): (Issue) -> Map<String, Either<ModuleError, ModuleResponse>> =
    lambda@{ updateIssue: Issue ->
        // Get issue again to retrieve all fields
        val issue = jiraClient.getIssue(updateIssue.key, "*all", "changelog")
        // Ignore issues where last action was a resolve
        if (issue.changeLog.entries.size > 0 && issue.changeLog.entries.last().items.any { it.field == "resolution" }) {
            return@lambda emptyMap()
        }
        val attachmentModule = AttachmentModule(
            run1IfShadow(config[Arisa.shadow], "DeleteAttachment", ::deleteAttachment.partially1(jiraClient)),
            config[Arisa.Modules.Attachment.extensionBlacklist]
        )
        val chkModule = CHKModule(
            run0IfShadow(
                config[Arisa.shadow],
                "UpdateCHK",
                ::updateCHK.partially1(issue).partially1(config[Arisa.CustomFields.chkField])
            )
        )
        val reopenAwaitingModule = ReopenAwaitingModule(
            run0IfShadow(config[Arisa.shadow], "ReopenIssue", ::reopenIssue.partially1(issue))
        )
        val piracyModule = PiracyModule(
            run0IfShadow(config[Arisa.shadow], "ResolveAsInvalid", ::resolveAs.partially1(issue).partially1("Invalid")),
            run0IfShadow(
                config[Arisa.shadow],
                "AddComment",
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.Piracy.piracyMessage])
            ),
            config[Arisa.Modules.Piracy.piracySignatures]
        )
        val removeTriagedMeqsModule = RemoveTriagedMeqsModule(
            run2IfShadow(config[Arisa.shadow], "UpdateCommentBody", ::updateCommentBody),
            config[Arisa.Modules.RemoveTriagedMeqs.meqsTags]
        )
        val futureVersionModule = FutureVersionModule(
            run1IfShadow(config[Arisa.shadow], "RemoveAffectedVersion", ::removeAffectedVersion.partially1(issue)),
            run1IfShadow(config[Arisa.shadow], "AddAffectedVersion", ::addAffectedVersion.partially1(issue)),
            run0IfShadow(
                config[Arisa.shadow],
                "AddComment",
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.FutureVersion.futureVersionMessage])
            )
        )
        val removeNonStaffMeqsModule = RemoveNonStaffMeqsModule(
            run2IfShadow(config[Arisa.shadow], "UpdateCommentBody", ::restrictCommentToGroup.partially2("staff"))
        )
        val emptyModule = EmptyModule(
            run0IfShadow(config[Arisa.shadow], "ResolveAsIncomplete", ::resolveAs.partially1(issue).partially1("Incomplete")),
            run0IfShadow(
                config[Arisa.shadow],
                "AddComment",
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.Empty.emptyMessage])
            )
        )
        val crashModule = CrashModule(
            run0IfShadow(config[Arisa.shadow], "ResolveAsInvalid", ::resolveAs.partially1(issue).partially1("Invalid")),
            run0IfShadow(config[Arisa.shadow], "ResolveAsDuplicae", ::resolveAs.partially1(issue).partially1("Duplicate")),
            run1IfShadow(config[Arisa.shadow], "AddDuplicatesLink", ::link.partially1(issue).partially1("Duplicate")),
            run0IfShadow(config[Arisa.shadow], "AddModdedComment", ::addComment.partially1(issue).partially1(config[Arisa.Modules.Crash.moddedMessage])),
            run1IfShadow(config[Arisa.shadow], "AddDuplicateComment") { key -> addComment(issue, config[Arisa.Modules.Crash.duplicateMessage].replace("{DUPLICATE}", key)) },
            config[Arisa.Modules.Crash.crashExtensions],
            config[Arisa.Modules.Crash.duplicates],
            config[Arisa.Modules.Crash.maxAttachmentAge]
        )
        // issue.project doesn't contain full project, which is needed for some modules.
        val project = try {
            jiraClient.getProject(issue.project.key)
        } catch (e: Exception) {
            log.error("Failed to get project of issue", e)
            null
        }

        mapOf(
            "Attachment" to runIfWhitelisted(issue, config[Arisa.Modules.Attachment.whitelist]) {
                attachmentModule(AttachmentModuleRequest(issue.attachments))
            },
            "CHK" to runIfWhitelisted(issue, config[Arisa.Modules.CHK.whitelist]) {
                chkModule(
                    CHKModuleRequest(
                        issue.key,
                        issue.getField(config[Arisa.CustomFields.chkField]) as? String?,
                        issue.getField(config[Arisa.CustomFields.confirmationField]) as? String?
                    )
                )
            },
            "ReopenAwaiting" to runIfWhitelisted(issue, config[Arisa.Modules.ReopenAwaiting.whitelist]) {
                reopenAwaitingModule(
                    ReopenAwaitingModuleRequest(
                        issue.resolution,
                        (issue.getField("created") as String).toInstant(),
                        (issue.getField("updated") as String).toInstant(),
                        issue.comments
                    )
                )
            },
            "Piracy" to runIfWhitelisted(issue, config[Arisa.Modules.Piracy.whitelist]) {
                piracyModule(
                    PiracyModuleRequest(
                        issue.getField("environment").toNullableString(),
                        issue.summary,
                        issue.description
                    )
                )
            },
            "RemoveTriagedMeqs" to runIfWhitelisted(issue, config[Arisa.Modules.ReopenAwaiting.whitelist]) {
                removeTriagedMeqsModule(
                    RemoveTriagedMeqsModuleRequest(
                        issue.getField(config[Arisa.CustomFields.mojangPriorityField]) as? String?,
                        issue.getField(config[Arisa.CustomFields.triagedTimeField]) as? String?,
                        issue.comments
                    )
                )
            },
            "FutureVersion" to runIfWhitelisted(issue, config[Arisa.Modules.FutureVersion.whitelist]) {
                futureVersionModule(
                    FutureVersionModuleRequest(
                        issue.versions,
                        project?.versions
                    )
                )
            },
            "RemoveNonStaffMeqs" to runIfWhitelisted(issue, config[Arisa.Modules.RemoveNonStaffMeqs.whitelist]) {
                removeNonStaffMeqsModule(
                    RemoveNonStaffMeqsModuleRequest(issue.comments)
                )
            },
            "Empty" to runIfWhitelisted(issue, config[Arisa.Modules.Empty.whitelist]) {
                emptyModule(
                    EmptyModuleRequest(
                        issue.attachments.size,
                        issue.description,
                        issue.getField("environment") as? String?
                    )
                )
            },
            "Crash" to runIfWhitelisted(issue, config[Arisa.Modules.Empty.whitelist]) {
                crashModule(
                    CrashModuleRequest(
                        issue.attachments,
                        issue.description,
                        issue.createdDate
                    )
                )
            }
        )
    }

private fun Any?.toNullableString(): String? = if (this is String) {
    this
} else {
    null
}

val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
private fun String.toInstant() = isoFormat.parse(this).toInstant()

private fun runIfWhitelisted(issue: Issue, projects: List<String>, body: () -> Either<ModuleError, ModuleResponse>) =
    if (isWhitelisted(projects, issue)) {
        body()
    } else {
        OperationNotNeededModuleResponse.left()
    }

private fun log0AndReturnUnit(method: String) = ({ Unit.right() }).also { log.info("[SHADOW] $method ran") }
private fun <T> log1AndReturnUnit(method: String) = { _: T -> Unit.right() }.also { log.info("[SHADOW] $method ran") }
private fun <T, U> log2AndReturnUnit(method: String) = { _: T, _: U -> Unit.right() }
    .also { log.info("[SHADOW] $method ran") }

private fun run0IfShadow(isShadow: Boolean, method: String, func: () -> Either<Throwable, Unit>) =
    if (!isShadow) {
        func
    } else {
        log0AndReturnUnit(method)
    }

private fun <T> run1IfShadow(
    isShadow: Boolean,
    method: String,
    func: (T) -> Either<Throwable, Unit>
) = if (!isShadow) {
    func
} else {
    log1AndReturnUnit(method)
}

private fun <T, U> run2IfShadow(
    isShadow: Boolean,
    method: String,
    func: (T, U) -> Either<Throwable, Unit>
) = if (!isShadow) {
    func
} else {
    log2AndReturnUnit(method)
}

fun isWhitelisted(projects: List<String>, issue: Issue) = projects.contains(issue.project.key)
