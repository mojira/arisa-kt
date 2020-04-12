package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.addAffectedVersion
import io.github.mojira.arisa.infrastructure.addComment
import io.github.mojira.arisa.infrastructure.addRestrictedComment
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
import io.github.mojira.arisa.modules.LanguageModule
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import me.urielsalis.mccrashlib.CrashReader
import net.rcarz.jiraclient.ChangeLogEntry
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

class ModuleExecutor(
    private val jiraClient: JiraClient,
    private val config: Config,
    private val cache: Cache
) {
    private val attachmentModule: AttachmentModule =
        AttachmentModule(config[Arisa.Modules.Attachment.extensionBlacklist])
    private val chkModule: CHKModule = CHKModule()
    private val crashModule: CrashModule = CrashModule(
        config[Arisa.Modules.Crash.crashExtensions],
        config[Arisa.Modules.Crash.duplicates],
        config[Arisa.Modules.Crash.maxAttachmentAge],
        CrashReader()
    )
    private val emptyModule: EmptyModule = EmptyModule()
    private val futureVersionModule: FutureVersionModule = FutureVersionModule()
    private val hideImpostorsModule: HideImpostorsModule = HideImpostorsModule()
    private val keepPrivateModule: KeepPrivateModule = KeepPrivateModule(config[Arisa.Modules.KeepPrivate.tag])
    private val languageModule: LanguageModule = LanguageModule()
    private val piracyModule: PiracyModule = PiracyModule(config[Arisa.Modules.Piracy.piracySignatures])
    private val removeNonStaffMeqsModule: RemoveNonStaffMeqsModule =
        RemoveNonStaffMeqsModule(config[Arisa.Modules.RemoveNonStaffMeqs.removalReason])
    private val removeTriagedMeqsModule: RemoveTriagedMeqsModule = RemoveTriagedMeqsModule(
        config[Arisa.Modules.RemoveTriagedMeqs.meqsTags],
        config[Arisa.Modules.RemoveTriagedMeqs.removalReason]
    )
    private val reopenAwaitingModule: ReopenAwaitingModule = ReopenAwaitingModule()
    private val revokeConfirmationModule: RevokeConfirmationModule = RevokeConfirmationModule()
    private val resolveTrash: ResolveTrashModule = ResolveTrashModule()

    fun execute(lastRun: Long): Boolean {
        var allModulesSuccessful = true
        val exec = ::executeModule.partially2(cache).partially2(lastRun).partially2 { allModulesSuccessful = false }

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
                    issue.getFieldAsString(config[Arisa.CustomFields.chkField]),
                    issue.getCustomField(config[Arisa.CustomFields.confirmationField]),
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
                    issue.getCustomField(config[Arisa.CustomFields.confirmationField]),
                    issue.getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
                    ::resolveAs.partially1(issue).partially1("Invalid"),
                    ::resolveAs.partially1(issue).partially1("Duplicate"),
                    ::link.partially1(issue).partially1("Duplicate"),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.Crash.moddedMessage]),
                    { key ->
                        addComment(
                            issue,
                            config[Arisa.Modules.Crash.duplicateMessage].format(key)
                        )
                    }
                )
            )
        }
        exec(Arisa.Modules.Empty) { issue ->
            "Empty" to emptyModule(
                EmptyModule.Request(
                    issue.attachments.size,
                    issue.description,
                    issue.getFieldAsString("environment"),
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
                        .map { v ->
                            FutureVersionModule.Version(
                                v.isReleased,
                                v.isArchived,
                                ::removeAffectedVersion.partially1(issue).partially1(v)
                            )
                        },
                    project?.versions
                        ?.map { v ->
                            FutureVersionModule.Version(
                                v.isReleased,
                                v.isArchived,
                                ::addAffectedVersion.partially1(issue).partially1(v)
                            )
                        },
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.FutureVersion.message])
                )
            )
        }
        exec(Arisa.Modules.HideImpostors) { issue ->
            "HideImpostors" to hideImpostorsModule(
                HideImpostorsModule.Request(
                    issue.comments
                        .map { c ->
                            HideImpostorsModule.Comment(
                                c.author.displayName,
                                getGroups(
                                    jiraClient,
                                    c.author.name
                                ).fold({ null }, { it }),
                                c.updatedDate.toInstant(),
                                c.visibility?.type,
                                c.visibility?.value,
                                ::restrictCommentToGroup.partially1(c).partially1("staff").partially1(null)
                            )
                        }
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
                    issue.getFieldAsString("environment"),
                    issue.summary,
                    issue.description,
                    ::resolveAs.partially1(issue).partially1("Invalid"),
                    ::addComment.partially1(issue).partially1(config[Arisa.Modules.Piracy.message])
                )
            )
        }
        exec(Arisa.Modules.Language) { issue ->
            "Language" to languageModule(
                LanguageModule.Request(
                    issue.summary,
                    issue.description,
                    { Unit.right() }, // ::resolveAs.partially1(issue).partially1("Invalid"),
                    { language ->
                        val code = language.isoCode639_1.toString()
                        val translatedMessage = config[Arisa.Modules.Language.messages][code]
                        val defaultMessage = config[Arisa.Modules.Language.defaultMessage]
                        val text = if (translatedMessage != null) config[Arisa.Modules.Language.messageFormat].format(translatedMessage, defaultMessage) else defaultMessage

                        addRestrictedComment(issue, text, "helper")
                    }
                )
            )
        }
        exec(Arisa.Modules.RemoveNonStaffMeqs) { issue ->
            "RemoveNonStaffMeqs" to removeNonStaffMeqsModule(
                RemoveNonStaffMeqsModule.Request(
                    issue.comments
                        .map { c ->
                            RemoveNonStaffMeqsModule.Comment(
                                c.body,
                                c.visibility?.type,
                                c.visibility?.value,
                                ::restrictCommentToGroup.partially1(c).partially1("staff")
                            )
                        }
                )
            )
        }
        exec(Arisa.Modules.RemoveTriagedMeqs) {
            "RemoveTriagedMeqs" to removeTriagedMeqsModule(
                RemoveTriagedMeqsModule.Request(
                    it.getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
                    it.getFieldAsString(config[Arisa.CustomFields.triagedTimeField]),
                    it.comments
                        .map { c ->
                            RemoveTriagedMeqsModule.Comment(
                                c.body,
                                ::updateCommentBody.partially1(c)
                            )
                        }
                )
            )
        }
        exec(Arisa.Modules.ReopenAwaiting) { issue ->
            "ReopenAwaiting" to reopenAwaitingModule(
                ReopenAwaitingModule.Request(
                    issue.resolution?.name,
                    (issue.getFieldAsString("created"))!!.toInstant(),
                    (issue.getFieldAsString("updated"))!!.toInstant(),
                    issue.comments
                        .map { c ->
                            ReopenAwaitingModule.Comment(
                                c.updatedDate.toInstant().toEpochMilli(),
                                c.createdDate.toInstant().toEpochMilli()
                            )
                        },
                    ::reopenIssue.partially1(issue)
                )
            )
        }
        exec(Arisa.Modules.RevokeConfirmation) { issue ->
            "RevokeConfirmation" to revokeConfirmationModule(
                RevokeConfirmationModule.Request(
                    issue.getCustomField(config[Arisa.CustomFields.confirmationField]),
                    issue.changeLog.entries
                        .flatMap { e ->
                            e.items
                                .map { i ->
                                    RevokeConfirmationModule.ChangeLogItem(
                                        i.field,
                                        i.toString,
                                        e.created.toInstant(),
                                        getGroups(
                                            jiraClient,
                                            e.author.name
                                        ).fold({ null }, { it })
                                    )
                                }
                        },
                    ::updateConfirmation.partially1(issue).partially1(config[Arisa.CustomFields.confirmationField])
                )
            )
        }
        exec(Arisa.Modules.ResolveTrash) { issue ->
            "ResolveTrash" to resolveTrash(
                ResolveTrashModule.Request(
                    issue.project.key,
                    ::resolveAs.partially1(issue).partially1("Invalid")
                )
            )
        }

        cache.clearQueryCache()
        return allModulesSuccessful
    }

    private fun executeModule(
        moduleConfig: Arisa.Modules.ModuleConfigSpec,
        cache: Cache,
        lastRun: Long,
        onModuleFail: () -> Unit,
        executeModule: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    ) {
        val projects = (config[moduleConfig.whitelist] ?: config[Arisa.Issues.projects])
        val resolutions = config[moduleConfig.resolutions].map(String::toLowerCase)

        val jql = config[moduleConfig.jql].format(lastRun)

        val issues = cache.getQuery(jql) ?: jiraClient
            .searchIssues(jql)
            .issues
            .map { jiraClient.getIssue(it.key, "*all", "changelog") } // Get issues again to retrieve all fields
            .filter(::lastActionWasAResolve)

        cache.addQuery(jql, issues)

        issues
            .filter { it.project.key in projects }
            .filter { it.resolution?.name?.toLowerCase() ?: "unresolved" in resolutions }
            .map { it.key to executeModule(it) }
            .forEach { (issue, response) ->
                response.second.fold({
                    when (it) {
                        is OperationNotNeededModuleResponse -> if (config[Arisa.logOperationNotNeeded]) log.info("[RESPONSE] [$issue] [${response.first}] Operation not needed")
                        is FailedModuleResponse -> {
                            onModuleFail()
                            for (exception in it.exceptions) {
                                log.error("[RESPONSE] [$issue] [${response.first}] Failed", exception)
                            }
                        }
                    }
                }, {
                    log.info("[RESPONSE] [$issue] [${response.first}] Successful")
                })
            }
    }

    private fun lastActionWasAResolve(issue: Issue): Boolean {
        val latestChange = issue.changeLog.entries.lastOrNull()

        return latestChange == null ||
                latestChange.isATransition() ||
                latestChange.wasNotDoneByTheBot() ||
                latestChange.noCommentAfterIt(issue)
    }

    private fun ChangeLogEntry.noCommentAfterIt(issue: Issue) =
        (issue.comments.isNotEmpty() && issue.comments.last().updatedDate > created)

    private fun ChangeLogEntry.wasNotDoneByTheBot() =
        author.name == config[Arisa.Credentials.username]

    private fun ChangeLogEntry.isATransition() =
        !items.any { it.field == "resolution" }

    private fun String.toInstant() = isoFormat.parse(this).toInstant()

    private fun Issue.getFieldAsString(field: String) = this.getField(field) as? String?

    private fun Issue.getCustomField(customField: String): String? =
        ((getField(customField)) as? JSONObject)?.get("value") as? String?

    private fun getSecurityLevelId(project: String) =
        config[Arisa.PrivateSecurityLevel.special][project] ?: config[Arisa.PrivateSecurityLevel.default]
}
