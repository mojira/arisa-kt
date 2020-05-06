package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.pipe
import arrow.syntax.function.pipe2
import arrow.syntax.function.pipe3
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkParam
import io.github.mojira.arisa.infrastructure.config.Arisa.Credentials
import io.github.mojira.arisa.infrastructure.config.Arisa.CustomFields
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.infrastructure.getLanguage
import io.github.mojira.arisa.infrastructure.jira.addAffectedVersion
import io.github.mojira.arisa.infrastructure.jira.addAffectedVersionById
import io.github.mojira.arisa.infrastructure.jira.addComment
import io.github.mojira.arisa.infrastructure.jira.addRestrictedComment
import io.github.mojira.arisa.infrastructure.jira.createLink
import io.github.mojira.arisa.infrastructure.jira.createLinkForTransfer
import io.github.mojira.arisa.infrastructure.jira.deleteAttachment
import io.github.mojira.arisa.infrastructure.jira.getAttachments
import io.github.mojira.arisa.infrastructure.jira.getCHK
import io.github.mojira.arisa.infrastructure.jira.getChangeLogEntries
import io.github.mojira.arisa.infrastructure.jira.getComments
import io.github.mojira.arisa.infrastructure.jira.getConfirmation
import io.github.mojira.arisa.infrastructure.jira.getCreated
import io.github.mojira.arisa.infrastructure.jira.getEnvironment
import io.github.mojira.arisa.infrastructure.jira.getIssueForLink
import io.github.mojira.arisa.infrastructure.jira.getLinked
import io.github.mojira.arisa.infrastructure.jira.getLinks
import io.github.mojira.arisa.infrastructure.jira.getPriority
import io.github.mojira.arisa.infrastructure.jira.getSecurityLevelId
import io.github.mojira.arisa.infrastructure.jira.getTriagedTime
import io.github.mojira.arisa.infrastructure.jira.getUpdated
import io.github.mojira.arisa.infrastructure.jira.getVersions
import io.github.mojira.arisa.infrastructure.jira.getVersionsGetField
import io.github.mojira.arisa.infrastructure.jira.removeAffectedVersion
import io.github.mojira.arisa.infrastructure.jira.reopenIssue
import io.github.mojira.arisa.infrastructure.jira.resolveAs
import io.github.mojira.arisa.infrastructure.jira.updateCHK
import io.github.mojira.arisa.infrastructure.jira.updateConfirmation
import io.github.mojira.arisa.infrastructure.jira.updateDescription
import io.github.mojira.arisa.infrastructure.jira.updateLinked
import io.github.mojira.arisa.infrastructure.jira.updateSecurity
import io.github.mojira.arisa.modules.AbstractTransferFieldModule
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.ConfirmParentModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.LanguageModule
import io.github.mojira.arisa.modules.Module
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReplaceTextModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.TransferLinksModule
import io.github.mojira.arisa.modules.TransferVersionsModule
import io.github.mojira.arisa.modules.UpdateLinkedModule
import me.urielsalis.mccrashlib.CrashReader
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import java.time.Instant
import java.time.temporal.ChronoUnit

class ModuleRegistry(jiraClient: JiraClient, private val config: Config) {
    data class Entry(
        val config: ModuleConfigSpec,
        val getJql: (lastRun: Instant) -> String,
        val execute: (issue: Issue, lastRun: Instant) -> Pair<String, Either<ModuleError, ModuleResponse>>
    )

    private val modules = mutableListOf<Entry>()

    fun getModules(): List<Entry> =
        modules

    private fun <T> register(
        name: String,
        config: ModuleConfigSpec,
        module: Module<T>,
        getJql: (lastRun: Instant) -> String = { "updated > ${it.toEpochMilli()}" },
        requestCreator: (Issue) -> T
    ) = register(name, config, module, getJql) { issue, _ -> requestCreator(issue) }

    private fun <T> register(
        name: String,
        config: ModuleConfigSpec,
        module: Module<T>,
        getJql: (lastRun: Instant) -> String = { "updated > $it" },
        requestCreator: (Issue, Instant) -> T
    ) = { issue: Issue, lastRun: Instant ->
        name to ({ lastRun pipe (issue pipe2 requestCreator) pipe module::invoke } pipe ::tryExecuteModule)
    } pipe (getJql pipe2 (config pipe3 ModuleRegistry::Entry)) pipe modules::add

    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }

    init {
        register(
            "Attachment", Modules.Attachment, AttachmentModule(config[Modules.Attachment.extensionBlacklist])
        ) { issue -> AttachmentModule.Request(issue.getAttachments(::deleteAttachment.partially1(jiraClient))) }

        register("CHK", Modules.CHK, CHKModule()) { issue ->
            CHKModule.Request(
                issue.getCHK(config),
                issue.getConfirmation(config),
                ::updateCHK.partially1(issue).partially1(config[CustomFields.chkField])
            )
        }

        register(
            "ConfirmParent",
            Modules.ConfirmParent,
            ConfirmParentModule(
                config[Modules.ConfirmParent.confirmationStatusWhitelist],
                config[Modules.ConfirmParent.targetConfirmationStatus],
                config[Modules.ConfirmParent.linkedThreshold]
            )
        ) { issue ->
            ConfirmParentModule.Request(
                issue.getConfirmation(config),
                issue.getLinked(config),
                ::updateConfirmation.partially1(issue).partially1(config[CustomFields.confirmationField])
            )
        }

        register(
            "Crash",
            Modules.Crash,
            CrashModule(
                config[Modules.Crash.crashExtensions],
                config[Modules.Crash.duplicates],
                config[Modules.Crash.maxAttachmentAge],
                CrashReader()
            )
        ) { issue ->
            CrashModule.Request(
                issue.getAttachments { Unit.right() },
                issue.description,
                issue.createdDate.toInstant(),
                issue.getConfirmation(config),
                issue.getPriority(config),
                ::resolveAs.partially1(issue).partially1("Invalid"),
                ::resolveAs.partially1(issue).partially1("Duplicate"),
                ::createLink.partially1(issue).partially1("Duplicate"),
                ::addComment.partially1(issue).partially1(config[Modules.Crash.moddedMessage]),
                { key -> addComment(issue, config[Modules.Crash.duplicateMessage].format(key)) }
            )
        }

        register("Empty", Modules.Empty, EmptyModule()) { issue, lastRun ->
            EmptyModule.Request(
                issue.createdDate.toInstant(),
                lastRun,
                issue.attachments.size,
                issue.description,
                issue.getEnvironment(),
                ::resolveAs.partially1(issue).partially1("Incomplete"),
                ::addComment.partially1(issue).partially1(config[Modules.Empty.message])
            )
        }

        register("FutureVersion", Modules.FutureVersion, FutureVersionModule()) { issue ->
            val project = jiraClient.getProject(issue.project.key)
            FutureVersionModule.Request(
                issue.getVersions(::removeAffectedVersion.partially1(issue)),
                project.getVersions(::addAffectedVersion.partially1(issue)),
                ::addComment.partially1(issue).partially1(config[Modules.FutureVersion.message])
            )
        }

        register("HideImpostors", Modules.HideImpostors, HideImpostorsModule()) { issue ->
            HideImpostorsModule.Request(issue.getComments(jiraClient))
        }

        register("KeepPrivate", Modules.KeepPrivate, KeepPrivateModule(config[Modules.KeepPrivate.tag])) { issue ->
            KeepPrivateModule.Request(
                issue.security?.id,
                issue.getSecurityLevelId(config),
                issue.comments.map { c -> c.body },
                ::updateSecurity.partially1(issue).partially1(issue.getSecurityLevelId(config)),
                ::addComment.partially1(issue).partially1(config[Modules.KeepPrivate.message])
            )
        }

        register("TransferVersions", Modules.TransferVersions, TransferVersionsModule()) { issue ->
            AbstractTransferFieldModule.Request(
                issue.key,
                issue.getLinks(jiraClient, ::addAffectedVersionById, ::getVersionsGetField),
                issue.versions.map { it.id }
            )
        }

        register(
            "TransferLinks",
            Modules.TransferLinks,
            TransferLinksModule()
        ) { issue ->
            val links = issue.getLinks<List<Link<*, LinkParam>>, LinkParam>(
                    jiraClient,
                    ::createLinkForTransfer,
                    ::getIssueForLink.partially1(jiraClient)
                )

            AbstractTransferFieldModule.Request(
                issue.key,
                links,
                links
            )
        }

        register("Piracy", Modules.Piracy, PiracyModule(config[Modules.Piracy.piracySignatures])) { issue ->
            PiracyModule.Request(
                issue.getEnvironment(),
                issue.summary,
                issue.description,
                ::resolveAs.partially1(issue).partially1("Invalid"),
                ::addComment.partially1(issue).partially1(config[Modules.Piracy.message])
            )
        }

        register(
            "Language", Modules.Language, LanguageModule(lengthThreshold = config[Modules.Language.lengthThreshold])
        ) { issue, lastRun ->
            LanguageModule.Request(
                issue.createdDate.toInstant(),
                lastRun,
                issue.summary,
                issue.description,
                issue.security?.id,
                issue.getSecurityLevelId(config),
                ::getLanguage.partially1(config[Credentials.dandelionToken]),
                { Unit.right() }, // ::resolveAs.partially1(issue).partially1("Invalid"),
                { language ->
                    // Should we move this?
                    val translatedMessage = config[Modules.Language.messages][language]
                    val defaultMessage = config[Modules.Language.defaultMessage]
                    val text =
                        if (translatedMessage != null) config[Modules.Language.messageFormat].format(
                            translatedMessage,
                            defaultMessage
                        ) else defaultMessage

                    addRestrictedComment(
                        issue,
                        text,
                        "helper"
                    )
                }
            )
        }

        register(
            "RemoveNonStaffMeqs",
            Modules.RemoveNonStaffMeqs,
            RemoveNonStaffMeqsModule(config[Modules.RemoveNonStaffMeqs.removalReason])
        ) { issue -> RemoveNonStaffMeqsModule.Request(issue.getComments(jiraClient)) }

        register(
            "RemoveTriagedMeqs",
            Modules.RemoveTriagedMeqs,
            RemoveTriagedMeqsModule(
                config[Modules.RemoveTriagedMeqs.meqsTags],
                config[Modules.RemoveTriagedMeqs.removalReason]
            )
        ) { issue ->
            RemoveTriagedMeqsModule.Request(
                issue.getPriority(config),
                issue.getTriagedTime(config),
                issue.getComments(jiraClient)
            )
        }

        register(
            "ReopenAwaiting",
            Modules.ReopenAwaiting,
            ReopenAwaitingModule(
                config[Modules.ReopenAwaiting.blacklistedRoles],
                config[Modules.ReopenAwaiting.blacklistedVisibilities]
            )
        ) { issue ->
            ReopenAwaitingModule.Request(
                issue.resolution?.name,
                issue.getCreated(),
                issue.getUpdated(),
                issue.getComments(jiraClient),
                issue.getChangeLogEntries(jiraClient),
                ::reopenIssue.partially1(issue)
            )
        }

        register("ReplaceText", Modules.ReplaceText, ReplaceTextModule()) { issue, lastRun ->
            ReplaceTextModule.Request(
                lastRun,
                issue.description,
                issue.getComments(jiraClient),
                ::updateDescription.partially1(issue)
            )
        }

        register("RevokeConfirmation", Modules.RevokeConfirmation, RevokeConfirmationModule()) { issue ->
            RevokeConfirmationModule.Request(
                issue.getConfirmation(config),
                issue.getChangeLogEntries(jiraClient),
                ::updateConfirmation.partially1(issue).partially1(config[CustomFields.confirmationField])
            )
        }

        register("ResolveTrash", Modules.ResolveTrash, ResolveTrashModule()) { issue ->
            ResolveTrashModule.Request(issue.project.key, ::resolveAs.partially1(issue).partially1("Invalid"))
        }

        register(
            "UpdateLinked",
            Modules.UpdateLinked,
            UpdateLinkedModule(config[Modules.UpdateLinked.updateInterval]),
            { lastRun ->
                val now = Instant.now()
                val intervalStart = now.minus(config[Modules.UpdateLinked.updateInterval], ChronoUnit.HOURS)
                val intervalEnd = intervalStart.minusMillis(now.toEpochMilli() - lastRun.toEpochMilli())
                return@register "updated > $lastRun OR (updated < ${intervalStart.toEpochMilli()} AND updated > ${intervalEnd.toEpochMilli()})"
            },
            { issue ->
                UpdateLinkedModule.Request(
                    issue.createdDate.toInstant(),
                    issue.getChangeLogEntries(jiraClient),
                    issue.getLinked(config),
                    ::updateLinked.partially1(issue).partially1(config[CustomFields.linked])
                )
            }
        )
    }
}
