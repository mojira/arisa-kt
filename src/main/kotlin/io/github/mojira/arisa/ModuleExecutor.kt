package io.github.mojira.arisa

import arrow.core.Either
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.addAffectedVersion
import io.github.mojira.arisa.infrastructure.addComment
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.deleteAttachment
import io.github.mojira.arisa.infrastructure.getGroups
import io.github.mojira.arisa.infrastructure.link
import io.github.mojira.arisa.infrastructure.removeAffectedVersion
import io.github.mojira.arisa.infrastructure.reopenIssue
import io.github.mojira.arisa.infrastructure.resolveAs
import io.github.mojira.arisa.infrastructure.restrictCommentToGroup
import io.github.mojira.arisa.infrastructure.updateCHK
import io.github.mojira.arisa.infrastructure.updateCommentBody
import io.github.mojira.arisa.infrastructure.updateConfirmation
import io.github.mojira.arisa.infrastructure.updateSecurity
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
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.HideImpostorsModuleRequest
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.KeepPrivateModuleRequest
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
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.RevokeConfirmationModuleRequest
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Timer
import kotlin.concurrent.schedule

class ModuleExecutor(
    private val jiraClient: JiraClient,
    private val config: Config
) {
    private val attachmentModule: AttachmentModule
    private val chkModule: CHKModule
    private val crashModule: CrashModule
    private val emptyModule: EmptyModule
    private val keepPrivateModule: KeepPrivateModule
    private val futureVersionModule: FutureVersionModule
    private val hideImpostorsModule: HideImpostorsModule
    private val piracyModule: PiracyModule
    private val removeNonStaffMeqsModule: RemoveNonStaffMeqsModule
    private val removeTriagedMeqsModule: RemoveTriagedMeqsModule
    private val reopenAwaitingModule: ReopenAwaitingModule
    private val revokeConfirmationModule: RevokeConfirmationModule

    private val ticketCache = mutableListOf<String>()
    private val ticketTimer = Timer("RemoveCachedTicket", true)

    fun execute() {
        // Cache issues returned by a query to avoid searching the same query for different modules
        val queryCache = mutableMapOf<String, List<Issue>>()
        val processedTickets = mutableMapOf<String, Boolean>()
        val exec = ::executeModule.partially2(queryCache).partially2(processedTickets)

        exec(Arisa.Modules.Attachment) {
            "Attachment" to attachmentModule(AttachmentModuleRequest(it.attachments))
        }
        exec(Arisa.Modules.CHK) {
            "CHK" to chkModule(
                CHKModuleRequest(
                    it,
                    it.getField(config[Arisa.CustomFields.chkField]) as? String?,
                    ((it.getField(config[Arisa.CustomFields.confirmationField])) as? JSONObject)?.get("value") as? String?
                )
            )
        }
        exec(Arisa.Modules.Crash) {
            "Crash" to crashModule(
                CrashModuleRequest(
                    it,
                    it.attachments,
                    it.description,
                    it.createdDate
                )
            )
        }
        exec(Arisa.Modules.Empty) {
            "Empty" to emptyModule(
                EmptyModuleRequest(
                    it,
                    it.attachments.size,
                    it.description,
                    it.getField("environment") as? String?
                )
            )
        }
        exec(Arisa.Modules.FutureVersion) {
            // issue.project doesn't contain versions
            val project = try {
                jiraClient.getProject(it.project.key)
            } catch (e: Exception) {
                log.error("Failed to get project of issue", e)
                null
            }
            "FutureVersion" to futureVersionModule(
                FutureVersionModuleRequest(
                    it,
                    it.versions,
                    project?.versions
                )
            )
        }
        exec(Arisa.Modules.HideImpostors) {
            "HideImpostors" to hideImpostorsModule(
                HideImpostorsModuleRequest(it.comments)
            )
        }
        exec(Arisa.Modules.KeepPrivate) {
            "KeepPrivate" to keepPrivateModule(
                KeepPrivateModuleRequest(
                    it,
                    it.security?.id,
                    config[Arisa.PrivateSecurityLevel.special].getOrDefault(it.project.key, config[Arisa.PrivateSecurityLevel.default]),
                    it.comments
                )
            )
        }
        exec(Arisa.Modules.Piracy) {
            "Piracy" to piracyModule(
                PiracyModuleRequest(
                    it,
                    it.getField("environment") as? String?,
                    it.summary,
                    it.description
                )
            )
        }
        exec(Arisa.Modules.RemoveNonStaffMeqs) {
            "RemoveNonStaffMeqs" to removeNonStaffMeqsModule(RemoveNonStaffMeqsModuleRequest(it.comments))
        }
        exec(Arisa.Modules.RemoveTriagedMeqs) {
            "RemoveTriagedMeqs" to removeTriagedMeqsModule(
                RemoveTriagedMeqsModuleRequest(
                    ((it.getField(config[Arisa.CustomFields.mojangPriorityField])) as? JSONObject)?.get("value") as? String?,
                    it.getField(config[Arisa.CustomFields.triagedTimeField]) as? String?,
                    it.comments
                )
            )
        }
        exec(Arisa.Modules.ReopenAwaiting) {
            "ReopenAwaiting" to reopenAwaitingModule(
                ReopenAwaitingModuleRequest(
                    it,
                    it.resolution,
                    (it.getField("created") as String).toInstant(),
                    (it.getField("updated") as String).toInstant(),
                    it.comments
                )
            )
        }
        exec(Arisa.Modules.RevokeConfirmation) {
            "RevokeConfirmation" to revokeConfirmationModule(
                RevokeConfirmationModuleRequest(
                    it,
                    ((it.getField(config[Arisa.CustomFields.confirmationField])) as? JSONObject)?.get("value") as? String?,
                    it.changeLog.entries
                )
            )
        }

        processedTickets
            .filter { (_, executed) -> !executed }
            .map { (ticket, _) -> ticket }
            .forEach { ticket ->
                ticketCache.add(ticket)
                ticketTimer.schedule(290_000) { ticketCache.remove(ticket) }
            }
    }

    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        queryCache: MutableMap<String, List<Issue>>,
        processedIssues: MutableMap<String, Boolean>,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        val projects = config[Arisa.Issues.projects]
            .filter { config[moduleConfig.whitelist] == null || config[moduleConfig.whitelist]!!.contains(it) }
            .joinToString(",")
        val resolutions = config[moduleConfig.resolutions].joinToString(",")
        val cachedTickets = ticketCache.joinToString(",")
        val combinedJql = "project in ($projects) AND key not in ($cachedTickets) AND resolution in ($resolutions) AND (${config[moduleConfig.jql]})"

        val issues = queryCache[combinedJql] ?: jiraClient
            .searchIssues(combinedJql)
            .issues
            .map { jiraClient.getIssue(it.key, "*all", "changelog") } // Get issues again to retrieve all fields
            .filter { issue ->
                // Ignore issues where last action was a resolve
                val latestChange = issue.changeLog.entries.lastOrNull()

                latestChange == null || // There is actually a entry
                    !latestChange.items.any { it.field == "resolution" } || // It was a transition
                    latestChange.author.name == config[Arisa.Credentials.username] || // The transition was not done by the bot
                    (issue.comments.isNotEmpty() && issue.comments.last().updatedDate > latestChange.created) // And there is no comment posted after that
            }
        queryCache[combinedJql] = issues

        issues
            .map { it.key to executeModule(it) }
            .forEach { (issue, response) ->
                response.second.fold({
                    processedIssues.putIfAbsent(issue, false)
                    when (it) {
                        is OperationNotNeededModuleResponse -> log.info("[RESPONSE] [$issue] [${response.first}] Operation not needed")
                        is FailedModuleResponse -> for (exception in it.exceptions) {
                            log.error("[RESPONSE] [$issue] [${response.first}] Failed", exception)
                        }
                    }
                }, {
                    processedIssues[issue] = true
                    log.info("[RESPONSE] [$issue] [${response.first}] Successful")
                })
            }
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private fun String.toInstant() = isoFormat.parse(this).toInstant()

    init {
        attachmentModule = AttachmentModule(
            ::deleteAttachment.partially1(jiraClient),
            config[Arisa.Modules.Attachment.extensionBlacklist]
        )

        chkModule = CHKModule(
            ::updateCHK.partially2(config[Arisa.CustomFields.chkField])
        )

        crashModule = CrashModule(
            ::resolveAs.partially2("Invalid"),
            ::resolveAs.partially2("Duplicate"),
            ::link.partially2("Duplicate"),
            ::addComment.partially2(config[Arisa.Modules.Crash.moddedMessage]),
            { issue, key -> addComment(issue, config[Arisa.Modules.Crash.duplicateMessage].replace("{DUPLICATE}", key)) },
            config[Arisa.Modules.Crash.crashExtensions],
            config[Arisa.Modules.Crash.duplicates],
            config[Arisa.Modules.Crash.maxAttachmentAge]
        )

        emptyModule = EmptyModule(
            ::resolveAs.partially2("Incomplete"),
            ::addComment.partially2(config[Arisa.Modules.Empty.message])
        )

        futureVersionModule = FutureVersionModule(
            ::removeAffectedVersion,
            ::addAffectedVersion,
            ::addComment.partially2(config[Arisa.Modules.FutureVersion.message])
        )

        hideImpostorsModule = HideImpostorsModule(
            ::getGroups.partially1(jiraClient),
            ::restrictCommentToGroup.partially2("staff")
        )

        keepPrivateModule = KeepPrivateModule(
            ::updateSecurity,
            ::addComment.partially2(config[Arisa.Modules.KeepPrivate.message]),
            config[Arisa.Modules.KeepPrivate.tag]
        )

        piracyModule = PiracyModule(
            ::resolveAs.partially2("Invalid"),
            ::addComment.partially2(config[Arisa.Modules.Piracy.message]),
            config[Arisa.Modules.Piracy.piracySignatures]
        )

        removeNonStaffMeqsModule = RemoveNonStaffMeqsModule(
            ::restrictCommentToGroup.partially2("staff"),
            config[Arisa.Modules.RemoveNonStaffMeqs.removalReason]
        )

        removeTriagedMeqsModule = RemoveTriagedMeqsModule(
            ::updateCommentBody,
            config[Arisa.Modules.RemoveTriagedMeqs.meqsTags],
            config[Arisa.Modules.RemoveTriagedMeqs.removalReason]
        )

        reopenAwaitingModule = ReopenAwaitingModule(
            ::reopenIssue
        )

        revokeConfirmationModule = RevokeConfirmationModule(
            ::getGroups.partially1(jiraClient),
            ::updateConfirmation.partially2(config[Arisa.CustomFields.confirmationField])
        )
    }
}
