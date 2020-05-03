package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.pipe
import arrow.syntax.function.pipe2
import arrow.syntax.function.pipe3
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.addAffectedVersion
import io.github.mojira.arisa.infrastructure.addAffectedVersionById
import io.github.mojira.arisa.infrastructure.addComment
import io.github.mojira.arisa.infrastructure.addRestrictedComment
import io.github.mojira.arisa.infrastructure.config.Arisa.Credentials
import io.github.mojira.arisa.infrastructure.config.Arisa.CustomFields
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.infrastructure.config.Arisa.PrivateSecurityLevel
import io.github.mojira.arisa.infrastructure.config.FieldType
import io.github.mojira.arisa.infrastructure.createLink
import io.github.mojira.arisa.infrastructure.deleteAttachment
import io.github.mojira.arisa.infrastructure.deleteLink
import io.github.mojira.arisa.infrastructure.getGroups
import io.github.mojira.arisa.infrastructure.getIssue
import io.github.mojira.arisa.infrastructure.getLanguage
import io.github.mojira.arisa.infrastructure.removeAffectedVersion
import io.github.mojira.arisa.infrastructure.reopenIssue
import io.github.mojira.arisa.infrastructure.resolveAs
import io.github.mojira.arisa.infrastructure.restrictCommentToGroup
import io.github.mojira.arisa.infrastructure.updateCHK
import io.github.mojira.arisa.infrastructure.updateCommentBody
import io.github.mojira.arisa.infrastructure.updateDescription
import io.github.mojira.arisa.infrastructure.updateFieldByLiteralValue
import io.github.mojira.arisa.infrastructure.updateFieldByValue
import io.github.mojira.arisa.infrastructure.updateSecurity
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
import io.github.mojira.arisa.modules.RevokeFieldChangesModule
import io.github.mojira.arisa.modules.TransferLinksModule
import io.github.mojira.arisa.modules.TransferVersionsModule
import io.github.mojira.arisa.modules.UpdateLinkedModule
import me.urielsalis.mccrashlib.CrashReader
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit

class ModuleRegistry(jiraClient: JiraClient, private val config: Config) {
    data class Entry(
        val config: ModuleConfigSpec,
        val getJql: (lastRun: Long) -> String,
        val execute: (Issue, Long) -> Pair<String, Either<ModuleError, ModuleResponse>>
    )

    private val modules = mutableListOf<Entry>()

    fun getModules(): List<Entry> =
        modules

    private fun <T> register(
        name: String,
        config: ModuleConfigSpec,
        module: Module<T>,
        getJql: (lastRun: Long) -> String = { "updated > $it" },
        requestCreator: (Issue) -> T
    ) = register(name, config, module, getJql) { issue, _ -> requestCreator(issue) }

    private fun <T> register(
        name: String,
        config: ModuleConfigSpec,
        module: Module<T>,
        getJql: (lastRun: Long) -> String = { "updated > $it" },
        requestCreator: (Issue, Long) -> T
    ) = { issue: Issue, lastRun: Long ->
        name to ({ lastRun pipe (issue pipe2 requestCreator) pipe module::invoke } pipe ::tryExecuteModule)
    } pipe (getJql pipe2 (config pipe3 ModuleRegistry::Entry)) pipe modules::add

    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private fun String.toInstant() = isoFormat.parse(this).toInstant()

    private fun Issue.getFieldAsString(field: String) = this.getField(field)?.toString()

    private fun Issue.getCustomField(customField: String): String? =
        ((getField(customField)) as? JSONObject)?.get("value") as? String?

    private fun getSecurityLevelId(project: String) =
        config[PrivateSecurityLevel.special][project] ?: config[PrivateSecurityLevel.default]

    private fun getUserGroups(jiraClient: JiraClient, username: String) = getGroups(
        jiraClient,
        username
    ).fold({ null }, { it })

    init {
        val getGroups = ::getUserGroups.partially1(jiraClient)

        register(
            "Attachment",
            Modules.Attachment,
            AttachmentModule(config[Modules.Attachment.extensionBlacklist])
        ) { issue ->
            AttachmentModule.Request(
                issue.attachments.map { a ->
                    AttachmentModule.Attachment(
                        a.fileName,
                        ::deleteAttachment.partially1(jiraClient).partially1(a)
                    )
                }
            )
        }

        register(
            "CHK",
            Modules.CHK,
            CHKModule()
        ) { issue ->
            CHKModule.Request(
                issue.getFieldAsString(config[CustomFields.chkField]),
                issue.getCustomField(config[CustomFields.confirmationField]),
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
                issue.getCustomField(config[CustomFields.confirmationField]),
                issue.getField(config[CustomFields.linked]) as? Double?,
                ::updateFieldByValue
                    .partially1(issue)
                    .partially1(config[CustomFields.confirmationField])
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
                issue.attachments
                    .map { a -> CrashModule.Attachment(a.fileName, a.createdDate, a::download) },
                issue.description,
                issue.createdDate,
                issue.getCustomField(config[CustomFields.confirmationField]),
                issue.getCustomField(config[CustomFields.mojangPriorityField]),
                ::resolveAs.partially1(issue).partially1("Invalid"),
                ::resolveAs.partially1(issue).partially1("Duplicate"),
                ::createLink.partially1(issue).partially1("Duplicate"),
                ::addComment.partially1(issue).partially1(config[Modules.Crash.moddedMessage]),
                { key ->
                    addComment(
                        issue,
                        config[Modules.Crash.duplicateMessage].format(key)
                    )
                }
            )
        }

        register(
            "Empty",
            Modules.Empty,
            EmptyModule()
        ) { issue, lastRun ->
            EmptyModule.Request(
                issue.createdDate.toInstant().toEpochMilli(),
                lastRun,
                issue.attachments.size,
                issue.description,
                issue.getFieldAsString("environment"),
                ::resolveAs.partially1(issue).partially1("Incomplete"),
                ::addComment.partially1(issue).partially1(config[Modules.Empty.message])
            )
        }

        register(
            "FutureVersion",
            Modules.FutureVersion,
            FutureVersionModule()
        ) { issue ->
            val project = jiraClient.getProject(issue.project.key)
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
                ::addComment.partially1(issue).partially1(config[Modules.FutureVersion.message])
            )
        }

        register(
            "HideImpostors",
            Modules.HideImpostors,
            HideImpostorsModule()
        ) { issue ->
            HideImpostorsModule.Request(
                issue.comments
                    .map { c ->
                        HideImpostorsModule.Comment(
                            c.author.displayName,
                            getGroups.partially1(c.author.name),
                            c.updatedDate.toInstant(),
                            c.visibility?.type,
                            c.visibility?.value,
                            ::restrictCommentToGroup.partially1(c).partially1("staff").partially1(c.body)
                        )
                    }
            )
        }

        register(
            "KeepPrivate",
            Modules.KeepPrivate,
            KeepPrivateModule(config[Modules.KeepPrivate.tag])
        ) { issue ->
            KeepPrivateModule.Request(
                issue.security?.id,
                getSecurityLevelId(issue.project.key),
                issue.comments.map { c -> c.body },
                ::updateSecurity.partially1(issue).partially1(getSecurityLevelId(issue.project.key)),
                ::addComment.partially1(issue).partially1(config[Modules.KeepPrivate.message])
            )
        }

        register(
            "TransferVersions",
            Modules.TransferVersions,
            TransferVersionsModule()
        ) { issue ->
            AbstractTransferFieldModule.Request(
                issue.key,
                issue.issueLinks
                    .map { link ->
                        AbstractTransferFieldModule.Link(
                            link.type.name,
                            link.outwardIssue != null,
                            (
                                    if (link.outwardIssue != null)
                                        link.outwardIssue
                                    else
                                        link.inwardIssue
                                    ) pipe { linkedIssue ->
                                AbstractTransferFieldModule.LinkedIssue(
                                    linkedIssue.key,
                                    linkedIssue.status.name,
                                    ::addAffectedVersionById.partially1(linkedIssue),
                                    {
                                        getIssue(jiraClient, linkedIssue.key) pipe { issueEither ->
                                            issueEither.fold(
                                                { it.left() },
                                                { issue ->
                                                    issue.versions
                                                        .map { it.id }
                                                        .right()
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    },
                issue.versions.map { it.id }
            )
        }

        register(
            "TransferLinks",
            Modules.TransferLinks,
            TransferLinksModule()
        ) { issue ->
            // TODO: move mapping functions to own file
            val links =
                issue.issueLinks
                    .map { link ->
                        AbstractTransferFieldModule.Link(
                            link.type.name,
                            link.outwardIssue != null,
                            (
                                    if (link.outwardIssue != null)
                                        link.outwardIssue
                                    else
                                        link.inwardIssue
                                    ) pipe { linkedIssue ->
                                AbstractTransferFieldModule.LinkedIssue<List<AbstractTransferFieldModule.Link<*, TransferLinksModule.LinkParam>>, TransferLinksModule.LinkParam>(
                                    linkedIssue.key,
                                    linkedIssue.status.name,
                                    { createLink(linkedIssue, it.type, it.issue) },
                                    {
                                        getIssue(jiraClient, linkedIssue.key) pipe { issueEither ->
                                            issueEither.fold(
                                                { it.left() },
                                                { issue ->
                                                    issue.issueLinks
                                                        .map { link ->
                                                            AbstractTransferFieldModule.Link(
                                                                link.type.name,
                                                                link.outwardIssue != null,
                                                                (
                                                                        if (link.outwardIssue != null)
                                                                            link.outwardIssue
                                                                        else
                                                                            link.inwardIssue
                                                                        ) pipe { linkedIssue ->
                                                                    AbstractTransferFieldModule.LinkedIssue<Nothing, TransferLinksModule.LinkParam>(
                                                                        linkedIssue.key,
                                                                        linkedIssue.status.name,
                                                                        { createLink(linkedIssue, it.type, it.issue) },
                                                                        { UnsupportedOperationException().left() }
                                                                    )
                                                                }
                                                            )
                                                        }
                                                        .right()
                                                }
                                            )
                                        }
                                    }
                                )
                            },
                            ::deleteLink.partially1(link)
                        )
                    }

            AbstractTransferFieldModule.Request(
                issue.key,
                links,
                links
            )
        }

        register(
            "Piracy",
            Modules.Piracy,
            PiracyModule(config[Modules.Piracy.piracySignatures])
        ) { issue ->
            PiracyModule.Request(
                issue.getFieldAsString("environment"),
                issue.summary,
                issue.description,
                ::resolveAs.partially1(issue).partially1("Invalid"),
                ::addComment.partially1(issue).partially1(config[Modules.Piracy.message])
            )
        }

        register(
            "Language",
            Modules.Language,
            LanguageModule(lengthThreshold = config[Modules.Language.lengthThreshold])
        ) { issue, lastRun ->
            LanguageModule.Request(
                issue.createdDate.toInstant().toEpochMilli(),
                lastRun,
                issue.summary,
                issue.description,
                issue.security?.id,
                getSecurityLevelId(issue.project.key),
                ::getLanguage.partially1(config[Credentials.dandelionToken]),
                { Unit.right() }, // ::resolveAs.partially1(issue).partially1("Invalid"),
                { language ->
                    val translatedMessage = config[Modules.Language.messages][language]
                    val defaultMessage = config[Modules.Language.defaultMessage]
                    val text =
                        if (translatedMessage != null) config[Modules.Language.messageFormat].format(
                            translatedMessage,
                            defaultMessage
                        ) else defaultMessage

                    addRestrictedComment(issue, text, "helper")
                }
            )
        }

        register(
            "RemoveNonStaffMeqs",
            Modules.RemoveNonStaffMeqs,
            RemoveNonStaffMeqsModule(config[Modules.RemoveNonStaffMeqs.removalReason])
        ) { issue ->
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
        }

        register(
            "RemoveTriagedMeqs",
            Modules.RemoveTriagedMeqs,
            RemoveTriagedMeqsModule(
                config[Modules.RemoveTriagedMeqs.meqsTags],
                config[Modules.RemoveTriagedMeqs.removalReason]
            )
        ) { issue ->
            RemoveTriagedMeqsModule.Request(
                issue.getCustomField(config[CustomFields.mojangPriorityField]),
                issue.getFieldAsString(config[CustomFields.triagedTimeField]),
                issue.comments
                    .map { c ->
                        RemoveTriagedMeqsModule.Comment(
                            c.body,
                            ::updateCommentBody.partially1(c)
                        )
                    }
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
                (issue.getFieldAsString("created"))!!.toInstant(),
                (issue.getFieldAsString("updated"))!!.toInstant(),
                issue.comments
                    .map { c ->
                        ReopenAwaitingModule.Comment(
                            c.updatedDate.toInstant().toEpochMilli(),
                            c.createdDate.toInstant().toEpochMilli(),
                            c.visibility?.type,
                            c.visibility?.value,
                            getGroups.partially1(c.author.name)
                        )
                    },
                issue.changeLog.entries
                    .flatMap { e ->
                        e.items
                            .map { i ->
                                ReopenAwaitingModule.ChangeLogItem(
                                    e.created.toInstant().toEpochMilli(),
                                    i.toString
                                )
                            }
                    },
                ::reopenIssue.partially1(issue)
            )
        }

        register(
            "ReplaceText",
            Modules.ReplaceText,
            ReplaceTextModule()
        ) { issue, lastRun ->
            ReplaceTextModule.Request(
                lastRun,
                issue.description,
                issue.comments
                    .map { c ->
                        ReplaceTextModule.Comment(
                            c.updatedDate.toInstant().toEpochMilli(),
                            c.body,
                            ::updateCommentBody.partially1(c)
                        )
                    },
                ::updateDescription.partially1(issue)
            )
        }

        register(
            "RevokeFieldChanges",
            Modules.RevokeFieldChanges,
            RevokeFieldChangesModule()
        ) { issue, lastRun ->
            RevokeFieldChangesModule.Request(
                lastRun,
                config[Modules.RevokeFieldChanges.fields].map { config ->
                    RevokeFieldChangesModule.Field(
                        config.fieldId,
                        config.fieldName,
                        when (config.fieldType) {
                            FieldType.VALUE -> issue.getCustomField(config.fieldId)
                            FieldType.DOUBLE -> (issue.getField(config.fieldId) as? Double?)?.toInt()?.toString()
                            FieldType.STRING -> issue.getFieldAsString(config.fieldId)
                        },
                        config.defaultValue,
                        config.permissionGroups,
                        config.message,
                        when (config.fieldType) {
                            FieldType.VALUE -> ::updateFieldByValue.partially1(issue).partially1(config.fieldId)
                            FieldType.STRING -> ::updateFieldByLiteralValue.partially1(issue).partially1(config.fieldId)
                            FieldType.DOUBLE -> { value -> updateFieldByLiteralValue(issue, config.fieldId, value?.toDouble()) }
                        }
                    )
                },
                issue.changeLog.entries
                    .flatMap { e ->
                        e.items
                            .map { i ->
                                RevokeFieldChangesModule.ChangeLogItem(
                                    i.field,
                                    i.toString,
                                    e.created.toInstant().toEpochMilli(),
                                    getGroups.partially1(e.author.name)
                                )
                            }
                    },
                ::addComment.partially1(issue)
            )
        }

        register(
            "ResolveTrash",
            Modules.ResolveTrash,
            ResolveTrashModule()
        ) { issue ->
            ResolveTrashModule.Request(
                issue.project.key,
                ::resolveAs.partially1(issue).partially1("Invalid")
            )
        }

        register(
            "UpdateLinked",
            Modules.UpdateLinked,
            UpdateLinkedModule(config[Modules.UpdateLinked.updateInterval]),
            { lastRun ->
                val now = Instant.now()
                val intervalStart = now.minus(config[Modules.UpdateLinked.updateInterval], ChronoUnit.HOURS)
                val intervalEnd = intervalStart.minusMillis(now.minusMillis(lastRun).toEpochMilli())
                return@register "updated > $lastRun OR (updated < ${intervalStart.toEpochMilli()} AND updated > ${intervalEnd.toEpochMilli()})"
            },
            { issue ->
                UpdateLinkedModule.Request(
                    issue.createdDate.toInstant(),
                    issue.changeLog.entries
                        .flatMap { e ->
                            e.items
                                .map { i ->
                                    UpdateLinkedModule.ChangeLogItem(
                                        i.field,
                                        e.created.toInstant(),
                                        i.fromString,
                                        i.toString
                                    )
                                }
                        },
                    issue.getField(config[CustomFields.linked]) as? Double?,
                    ::updateFieldByLiteralValue.partially1(issue).partially1(config[CustomFields.linked])
                )
            }
        )
    }
}
