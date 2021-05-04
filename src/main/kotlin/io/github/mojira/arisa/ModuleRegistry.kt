package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.left
import arrow.syntax.function.partially1
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.config.Arisa.Credentials
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.infrastructure.getLanguage
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CommandModule
import io.github.mojira.arisa.modules.ConfirmParentModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.DuplicateMessageModule
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.KeepPlatformModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.LanguageModule
import io.github.mojira.arisa.modules.MissingCrashModule
import io.github.mojira.arisa.modules.Module
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.MultiplePlatformsModule
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.PrivacyModule
import io.github.mojira.arisa.modules.PrivateDuplicateModule
import io.github.mojira.arisa.modules.RemoveIdenticalLinkModule
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule
import io.github.mojira.arisa.modules.RemoveSpamModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.RemoveVersionModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReplaceTextModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.TransferLinksModule
import io.github.mojira.arisa.modules.TransferVersionsModule
import io.github.mojira.arisa.modules.UpdateLinkedModule
import me.urielsalis.mccrashlib.CrashReader
import java.time.Instant
import java.time.temporal.ChronoUnit

val DEFAULT_JQL = { lastRun: Instant -> "updated > ${lastRun.toEpochMilli()}" }

class ModuleRegistry(private val config: Config) {
    data class Entry(
        val name: String,
        val config: ModuleConfigSpec,
        val getJql: (lastRun: Instant) -> String,
        val execute: (issue: Issue, lastRun: Instant) -> Pair<String, Either<ModuleError, ModuleResponse>>
    )

    private val modules = mutableListOf<Entry>()

    fun getAllModules(): List<Entry> = modules

    fun getEnabledModules(): List<Entry> = modules.filter(::isModuleEnabled)

    private fun isModuleEnabled(module: Entry) =
        // If arisa.debug.enabledModules is defined, return whether that module is in that list.
        // If it's not defined, return whether this module is enabled in the module config.
        config[Arisa.Debug.enabledModules]?.contains(module.name) ?: config[module.config.enabled]

    private fun register(
        moduleConfig: ModuleConfigSpec,
        module: Module,
        getJql: (lastRun: Instant) -> String = DEFAULT_JQL
    ) {
        val moduleName = moduleConfig::class.simpleName!!

        modules.add(
            Entry(
                moduleName,
                moduleConfig,
                addDebugToJql(getJql),
                getModuleResult(moduleName, module)
            )
        )
    }

    private fun getModuleResult(moduleName: String, module: Module) = { issue: Issue, lastRun: Instant ->
        moduleName to tryExecuteModule { module(issue, lastRun) }
    }

    private fun addDebugToJql(getJql: (Instant) -> String) = { lastRun: Instant ->
        with(config[Arisa.Debug.ticketWhitelist]) {
            if (this == null) getJql(lastRun)
            else "key IN (${ joinToString(",") }) AND (${ getJql(lastRun) })"
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }

    init {
        register(
            Modules.Attachment,
            AttachmentModule(config[Modules.Attachment.extensionBlacklist], config[Modules.Attachment.comment])
        )

        register(Modules.CHK, CHKModule())

        register(
            Modules.ConfirmParent,
            ConfirmParentModule(
                config[Modules.ConfirmParent.confirmationStatusWhitelist],
                config[Modules.ConfirmParent.targetConfirmationStatus],
                config[Modules.ConfirmParent.linkedThreshold]
            )
        )

        register(
            Modules.MultiplePlatforms,
            MultiplePlatformsModule(
                config[Modules.MultiplePlatforms.platformWhitelist],
                config[Modules.MultiplePlatforms.targetPlatform],
                config[Modules.MultiplePlatforms.transferredPlatformBlacklist],
                config[Modules.MultiplePlatforms.keepPlatformTag]
            )
        )

        register(
            Modules.KeepPlatform,
            KeepPlatformModule(
                config[Modules.KeepPlatform.keepPlatformTag]
            )
        )

        register(
            Modules.Crash,
            CrashModule(
                config[Modules.Crash.crashExtensions],
                config[Modules.Crash.duplicates],
                CrashReader(),
                config[Modules.Crash.duplicateMessage],
                config[Modules.Crash.moddedMessage]
            )
        )

        register(
            Modules.MissingCrash,
            MissingCrashModule(
                config[Modules.MissingCrash.crashExtensions],
                CrashReader(),
                config[Modules.MissingCrash.message]
            )
        )

        register(Modules.Empty, EmptyModule(config[Modules.Empty.message]))

        register(
            Modules.DuplicateMessage,
            DuplicateMessageModule(
                config[Modules.DuplicateMessage.commentDelayMinutes],
                config[Modules.DuplicateMessage.message],
                config[Modules.DuplicateMessage.forwardMessage],
                config[Modules.DuplicateMessage.ticketMessages],
                config[Modules.DuplicateMessage.privateMessage],
                config[Modules.DuplicateMessage.preventMessageTags],
                config[Modules.DuplicateMessage.resolutionMessages]
            )
        ) { lastRun ->
            val checkStart = lastRun
                .minus(config[Modules.DuplicateMessage.commentDelayMinutes], ChronoUnit.MINUTES)
            val checkEnd = Instant.now()
                .minus(config[Modules.DuplicateMessage.commentDelayMinutes], ChronoUnit.MINUTES)
            "updated > ${checkStart.toEpochMilli()} AND updated < ${checkEnd.toEpochMilli()}"
        }

        register(Modules.HideImpostors, HideImpostorsModule())

        register(
            Modules.RemoveSpam,
            RemoveSpamModule(
                config[Modules.RemoveSpam.patterns]
            )
        )

        register(
            Modules.KeepPrivate, KeepPrivateModule(
                config[Modules.KeepPrivate.tag],
                config[Modules.KeepPrivate.message]
            )
        )

        register(
            Modules.PrivateDuplicate, PrivateDuplicateModule(
                config[Modules.PrivateDuplicate.tag]
            )
        )

        register(Modules.TransferVersions, TransferVersionsModule())

        register(
            Modules.TransferLinks,
            TransferLinksModule()
        )

        register(
            Modules.Piracy, PiracyModule(
                config[Modules.Piracy.piracySignatures],
                config[Modules.Piracy.message]
            )
        )

        register(
            Modules.Privacy,
            PrivacyModule(
                config[Modules.Privacy.message],
                config[Modules.Privacy.commentNote],
                config[Modules.Privacy.allowedEmailRegex].map(String::toRegex)
            )
        )

        register(
            Modules.Language,
            LanguageModule(
                config[Modules.Language.allowedLanguages],
                config[Modules.Language.lengthThreshold],
                ::getLanguage.partially1(config[Credentials.dandelionToken])
            )
        )

        register(Modules.RemoveIdenticalLink, RemoveIdenticalLinkModule())

        register(
            Modules.RemoveNonStaffMeqs,
            RemoveNonStaffMeqsModule(config[Modules.RemoveNonStaffMeqs.removalReason])
        )

        register(
            Modules.RemoveTriagedMeqs,
            RemoveTriagedMeqsModule(
                config[Modules.RemoveTriagedMeqs.meqsTags],
                config[Modules.RemoveTriagedMeqs.removalReason]
            )
        )

        register(
            Modules.ReopenAwaiting,
            ReopenAwaitingModule(
                config[Modules.ReopenAwaiting.blacklistedRoles],
                config[Modules.ReopenAwaiting.blacklistedVisibilities],
                config[Modules.ReopenAwaiting.softARDays],
                config[Modules.ReopenAwaiting.keepARTag],
                config[Modules.ReopenAwaiting.onlyOPTag],
                config[Modules.ReopenAwaiting.message]
            )
        )

        register(Modules.ReplaceText, ReplaceTextModule())

        register(Modules.RevokeConfirmation, RevokeConfirmationModule())

        register(Modules.ResolveTrash, ResolveTrashModule())

        register(
            Modules.FutureVersion,
            FutureVersionModule(
                config[Modules.FutureVersion.message],
                config[Modules.FutureVersion.panel]
            )
        )

        register(
            Modules.RemoveVersion,
            RemoveVersionModule(
                config[Modules.RemoveVersion.message]
            )
        )

        register(
            Modules.Command,
            CommandModule(config[Modules.Command.commandPrefix])
        )

        register(
            Modules.UpdateLinked,
            UpdateLinkedModule(config[Modules.UpdateLinked.updateIntervalHours])
        ) { lastRun ->
            val now = Instant.now()
            val intervalStart = now.minus(config[Modules.UpdateLinked.updateIntervalHours], ChronoUnit.HOURS)
            val intervalEnd = intervalStart.minusMillis(now.toEpochMilli() - lastRun.toEpochMilli())
            return@register "updated > ${lastRun.toEpochMilli()} OR (updated < ${intervalStart.toEpochMilli()}" +
                    " AND updated > ${intervalEnd.toEpochMilli()})"
        }
    }
}
