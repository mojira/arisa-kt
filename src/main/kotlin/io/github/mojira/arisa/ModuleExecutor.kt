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
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
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

    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        queryCache: MutableMap<String, List<Issue>>,
        processedIssues: MutableMap<String, Boolean>,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        val projects = config[Arisa.Issues.projects]
            .filter { config[moduleConfig.whitelist] == null || config[moduleConfig.whitelist]!!.contains(it) }
            .joinToString(",")
        val resolutions = config[moduleConfig.resolutions].joinToString(",") { "\"$it\"" }
        val cachedTickets = if (ticketCache.isEmpty()) "" else "AND key not in (${ticketCache.joinToString(",")})"
        val combinedJql = "project in ($projects) $cachedTickets AND resolution in ($resolutions) AND (${config[moduleConfig.jql]})"

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
                        is OperationNotNeededModuleResponse -> if (config[Arisa.logOperationNotNeeded]) log.info("[RESPONSE] [$issue] [${response.first}] Operation not needed")
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

    fun execute() {
        // Cache issues returned by a query to avoid searching the same query for different modules
        val queryCache = mutableMapOf<String, List<Issue>>()
        val processedTickets = mutableMapOf<String, Boolean>()
        val exec = ::executeModule.partially2(queryCache).partially2(processedTickets)

        exec(Arisa.Modules.Attachment) { issue ->
            "Attachment" to attachmentModule(
                AttachmentModule.Request(
                    issue.attachments
                        .map { a ->
                            AttachmentModule.Attachment(
                                a.fileName,
                                ::deleteAttachment.partially1(jiraClient).partially1(a)
                            )
                        }
                )
            )
        }
        exec(Arisa.Modules.CHK) { issue ->
            "CHK" to chkModule(
                CHKModule.Request(
                    issue.getField(config[Arisa.CustomFields.chkField]) as? String?,
                    ((issue.getField(config[Arisa.CustomFields.confirmationField])) as? JSONObject)?.get("value") as? String?,
                    ::updateCHK.partially1(issue).partially1(config[Arisa.CustomFields.chkField])
                )
            )
        }
        exec(Arisa.Modules.Crash) { issue ->
            "Crash" to crashModule(
                CrashModule.Request(
                    issue.attachments
                        .map { a -> CrashModule.Attachment(a.fileName, a.createdDate, a.download()) },
                    issue.description,
                    issue.createdDate,
                    ::resolveAs.partially1(issue).partially1("Invalid"),
                    ::resolveAs.partially1(issue).partially1("Duplicate"),
                    ::link.partially1(issue).partially1("Duplicate"),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.Crash.moddedMessage]),
                    { key -> addComment(issue, config[Arisa.Modules.Crash.duplicateMessage].replace("{DUPLICATE}", key)) }
                )
            )
        }
        exec(Arisa.Modules.Empty) { issue ->
            "Empty" to emptyModule(
                EmptyModule.Request(
                    issue.attachments.size,
                    issue.description,
                    issue.getField("environment") as? String?,
                    ::resolveAs.partially1(issue).partially1("Incomplete"),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.Empty.message])
                )
            )
        }
        exec(Arisa.Modules.FutureVersion) { issue ->
            // issue.project doesn't contain versions
            val project = try {
                jiraClient.getProject(issue.project.key)
            } catch (e: Exception) {
                log.error("Failed to get project of issue", e)
                null
            }
            "FutureVersion" to futureVersionModule(
                FutureVersionModule.Request(
                    issue.versions
                        .map { v -> FutureVersionModule.Version(
                            v.isReleased,
                            v.isArchived,
                            ::removeAffectedVersion.partially1(issue).partially1(v))
                        },
                    project?.versions
                        ?.map { v -> FutureVersionModule.Version(
                            v.isReleased,
                            v.isArchived,
                            ::addAffectedVersion.partially1(issue).partially1(v))
                        },
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.FutureVersion.message])
                )
            )
        }
        exec(Arisa.Modules.HideImpostors) { issue ->
            "HideImpostors" to hideImpostorsModule(
                HideImpostorsModule.Request(
                    issue.comments
                        .map { c -> HideImpostorsModule.Comment(
                            c.author.displayName,
                            getGroups(jiraClient, c.author.name).fold({ null }, { it }),
                            c.updatedDate.toInstant(),
                            c.visibility?.type,
                            c.visibility?.value,
                            ::restrictCommentToGroup.partially1(c).partially1("staff").partially1(null)
                        ) }
                )
            )
        }
        exec(Arisa.Modules.KeepPrivate) { issue ->
            "KeepPrivate" to keepPrivateModule(
                KeepPrivateModule.Request(
                    issue.security?.id,
                    getSecurityLevelId(issue.project.key),
                    issue.comments.map { c -> c.body },
                    ::updateSecurity.partially1(issue).partially1(getSecurityLevelId(issue.project.key)),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.KeepPrivate.message])
                )
            )
        }
        exec(Arisa.Modules.Piracy) { issue ->
            "Piracy" to piracyModule(
                PiracyModule.Request(
                    issue.getField("environment") as? String?,
                    issue.summary,
                    issue.description,
                    ::resolveAs.partially1(issue).partially1("Invalid"),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.Piracy.message])
                )
            )
        }
        exec(Arisa.Modules.RemoveNonStaffMeqs) { issue ->
            "RemoveNonStaffMeqs" to removeNonStaffMeqsModule(
                RemoveNonStaffMeqsModule.Request(
                    issue.comments
                        .map { c -> RemoveNonStaffMeqsModule.Comment(
                            c.body,
                            c.visibility?.type,
                            c.visibility?.value,
                            ::restrictCommentToGroup.partially1(c).partially1("staff")
                        ) }
                )
            )
        }
        exec(Arisa.Modules.RemoveTriagedMeqs) {
            "RemoveTriagedMeqs" to removeTriagedMeqsModule(
                RemoveTriagedMeqsModule.Request(
                    ((it.getField(config[Arisa.CustomFields.mojangPriorityField])) as? JSONObject)?.get("value") as? String?,
                    it.getField(config[Arisa.CustomFields.triagedTimeField]) as? String?,
                    it.comments
                        .map { c -> RemoveTriagedMeqsModule.Comment(
                            c.body,
                            ::updateCommentBody.partially1(c)
                        ) }
                )
            )
        }
        exec(Arisa.Modules.ReopenAwaiting) { issue ->
            "ReopenAwaiting" to reopenAwaitingModule(
                ReopenAwaitingModule.Request(
                    issue.resolution.name,
                    (issue.getField("created") as String).toInstant(),
                    (issue.getField("updated") as String).toInstant(),
                    issue.comments
                        .map { c -> ReopenAwaitingModule.Comment(
                            c.updatedDate.toInstant().toEpochMilli(),
                            c.createdDate.toInstant().toEpochMilli()
                        ) },
                    ::reopenIssue.partially1(issue)
                )
            )
        }
        exec(Arisa.Modules.RevokeConfirmation) { issue ->
            "RevokeConfirmation" to revokeConfirmationModule(
                RevokeConfirmationModule.Request(
                    ((issue.getField(config[Arisa.CustomFields.confirmationField])) as? JSONObject)?.get("value") as? String?,
                    issue.changeLog.entries
                        .flatMap { e -> e.items
                            .map { i -> RevokeConfirmationModule.ChangeLogItem(
                                i.field,
                                i.toString,
                                e.created.toInstant(),
                                getGroups(jiraClient, e.author.name).fold({ null }, { it })
                            ) }
                        },
                    ::updateConfirmation.partially1(issue).partially1(config[Arisa.CustomFields.confirmationField])
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

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private fun String.toInstant() = isoFormat.parse(this).toInstant()

    private fun getSecurityLevelId(project: String) =
        config[Arisa.PrivateSecurityLevel.special][project] ?: config[Arisa.PrivateSecurityLevel.default]

    init {
        attachmentModule = AttachmentModule(config[Arisa.Modules.Attachment.extensionBlacklist])

        chkModule = CHKModule()

        crashModule = CrashModule(
            config[Arisa.Modules.Crash.crashExtensions],
            config[Arisa.Modules.Crash.duplicates],
            config[Arisa.Modules.Crash.maxAttachmentAge]
        )

        emptyModule = EmptyModule()

        futureVersionModule = FutureVersionModule()

        hideImpostorsModule = HideImpostorsModule()

        keepPrivateModule = KeepPrivateModule(config[Arisa.Modules.KeepPrivate.tag])

        piracyModule = PiracyModule(config[Arisa.Modules.Piracy.piracySignatures])

        removeNonStaffMeqsModule = RemoveNonStaffMeqsModule(config[Arisa.Modules.RemoveNonStaffMeqs.removalReason])

        removeTriagedMeqsModule = RemoveTriagedMeqsModule(
            config[Arisa.Modules.RemoveTriagedMeqs.meqsTags],
            config[Arisa.Modules.RemoveTriagedMeqs.removalReason]
        )

        reopenAwaitingModule = ReopenAwaitingModule()

        revokeConfirmationModule = RevokeConfirmationModule()
    }
}
