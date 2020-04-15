package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.left
import arrow.syntax.function.partially1
import arrow.syntax.function.pipe
import arrow.syntax.function.pipe2
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.Module
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.UpdateLinkedModule
import me.urielsalis.mccrashlib.CrashReader
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.sf.json.JSONObject
import java.text.SimpleDateFormat

class ModuleRegistry(jiraClient: JiraClient, private val config: Config) {
    data class Entry(
        val config: ModuleConfigSpec,
        val execute: (Issue) -> Pair<String, Either<ModuleError, ModuleResponse>>
    )

    private val modules = mutableListOf<Entry>()

    fun getModules() =
        modules.toList()

    private fun <T> register(
        name: String,
        config: ModuleConfigSpec,
        module: Module<T>,
        requestCreator: (Issue) -> T
    ) = { issue: Issue ->
        name to ({ issue pipe requestCreator pipe module::invoke } pipe ::tryExecuteModule)
    } pipe (config pipe2 ::Entry) pipe modules::add

    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private fun String.toInstant() = isoFormat.parse(this).toInstant()

    private fun Issue.getFieldAsString(field: String) = this.getField(field) as? String?

    private fun Issue.getCustomField(customField: String): String? =
        ((getField(customField)) as? JSONObject)?.get("value") as? String?

    private fun getSecurityLevelId(project: String) =
        config[Arisa.PrivateSecurityLevel.special][project] ?: config[Arisa.PrivateSecurityLevel.default]

    init {
        register(
            "Attachment",
            Arisa.Modules.Attachment,
            AttachmentModule(config[Arisa.Modules.Attachment.extensionBlacklist])
        )
        { issue ->
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
            Arisa.Modules.CHK,
            CHKModule()
        ) { issue ->
            CHKModule.Request(
                issue.getFieldAsString(config[Arisa.CustomFields.chkField]),
                issue.getCustomField(config[Arisa.CustomFields.confirmationField]),
                ::updateCHK.partially1(issue).partially1(config[Arisa.CustomFields.chkField])
            )
        }

        register(
            "Crash",
            Arisa.Modules.Crash,
            CrashModule(
                config[Arisa.Modules.Crash.crashExtensions],
                config[Arisa.Modules.Crash.duplicates],
                config[Arisa.Modules.Crash.maxAttachmentAge],
                CrashReader()
            )
        )
        { issue ->
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
        }

        register(
            "Empty",
            Arisa.Modules.Empty,
            EmptyModule()
        ) { issue ->
            EmptyModule.Request(
                issue.attachments.size,
                issue.description,
                issue.getFieldAsString("environment"),
                ::resolveAs.partially1(issue).partially1("Incomplete"),
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.Empty.message])
            )
        }

        register(
            "FutureVersion",
            Arisa.Modules.FutureVersion,
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
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.FutureVersion.message])
            )
        }

        register(
            "HideImpostors",
            Arisa.Modules.HideImpostors,
            HideImpostorsModule()
        ) { issue ->
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
        }

        register(
            "KeepPrivate",
            Arisa.Modules.KeepPrivate,
            KeepPrivateModule(config[Arisa.Modules.KeepPrivate.tag])
        ) { issue ->
            KeepPrivateModule.Request(
                issue.security?.id,
                getSecurityLevelId(issue.project.key),
                issue.comments.map { c -> c.body },
                ::updateSecurity.partially1(issue).partially1(getSecurityLevelId(issue.project.key)),
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.KeepPrivate.message])
            )
        }

        register(
            "Piracy",
            Arisa.Modules.Piracy,
            PiracyModule(config[Arisa.Modules.Piracy.piracySignatures])
        ) { issue ->
            PiracyModule.Request(
                issue.getFieldAsString("environment"),
                issue.summary,
                issue.description,
                ::resolveAs.partially1(issue).partially1("Invalid"),
                ::addComment.partially1(issue).partially1(config[Arisa.Modules.Piracy.message])
            )
        }

        register(
            "RemoveNonStaffMeqs",
            Arisa.Modules.RemoveNonStaffMeqs,
            RemoveNonStaffMeqsModule(config[Arisa.Modules.RemoveNonStaffMeqs.removalReason])
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
            Arisa.Modules.RemoveTriagedMeqs,
            RemoveTriagedMeqsModule(
                config[Arisa.Modules.RemoveTriagedMeqs.meqsTags],
                config[Arisa.Modules.RemoveTriagedMeqs.removalReason]
            )
        ) { issue ->
            RemoveTriagedMeqsModule.Request(
                issue.getCustomField(config[Arisa.CustomFields.mojangPriorityField]),
                issue.getFieldAsString(config[Arisa.CustomFields.triagedTimeField]),
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
            Arisa.Modules.ReopenAwaiting,
            ReopenAwaitingModule()
        ) { issue ->
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
        }

        register(
            "RevokeConfirmation",
            Arisa.Modules.RevokeConfirmation,
            RevokeConfirmationModule()
        ) { issue ->
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
                ::updateConfirmation.partially1(issue)
                    .partially1(config[Arisa.CustomFields.confirmationField])
            )
        }

        register(
            "ResolveTrash",
            Arisa.Modules.ResolveTrash,
            ResolveTrashModule()
        ) { issue ->
            ResolveTrashModule.Request(
                issue.project.key,
                ::resolveAs.partially1(issue).partially1("Invalid")
            )
        }

        register(
            "UpdateLinked",
            Arisa.Modules.UpdateLinked,
            UpdateLinkedModule()
        ) { issue ->
            UpdateLinkedModule.Request(
                issue.issueLinks
                    .map { it.type.name },
                issue.getField(config[Arisa.CustomFields.linked]) as? Double?,
                ::updateLinked.partially1(issue).partially1(config[Arisa.CustomFields.linked])
            )
        }
    }
}