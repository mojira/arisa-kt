package io.github.mojira.arisa

import arrow.core.Either
import arrow.core.left
import arrow.syntax.function.partially1
import arrow.syntax.function.pipe
import arrow.syntax.function.pipe2
import arrow.syntax.function.pipe3
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.Arisa.Credentials
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules
import io.github.mojira.arisa.infrastructure.config.Arisa.Modules.ModuleConfigSpec
import io.github.mojira.arisa.infrastructure.getLanguage
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.ConfirmParentModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.DuplicateMessageModule
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
import java.time.Instant
import java.time.temporal.ChronoUnit

val DEFAULT_JQL = { lastRun: Instant -> "updated > ${lastRun.toEpochMilli()}" }

class ModuleRegistry(private val config: Config) {
    data class Entry(
        val config: ModuleConfigSpec,
        val getJql: (lastRun: Instant) -> String,
        val execute: (issue: Issue, lastRun: Instant) -> Pair<String, Either<ModuleError, ModuleResponse>>
    )

    private val modules = mutableListOf<Entry>()

    fun getModules(config: Config): List<Entry> {
        val onlyModules = modules
            .filter { config[it.config.only] }
        return if (onlyModules.isEmpty()) {
            modules
        } else {
            onlyModules
        }
    }

    private fun register(
        config: ModuleConfigSpec,
        module: Module,
        getJql: (lastRun: Instant) -> String = DEFAULT_JQL
    ) = { issue: Issue, lastRun: Instant ->
        config::class.simpleName!! to
                ({ lastRun pipe (issue pipe2 module::invoke) } pipe ::tryExecuteModule)
    } pipe (getJql pipe2 (config pipe3 ModuleRegistry::Entry)) pipe modules::add

    @Suppress("TooGenericExceptionCaught")
    private fun tryExecuteModule(executeModule: () -> Either<ModuleError, ModuleResponse>) = try {
        executeModule()
    } catch (e: Throwable) {
        FailedModuleResponse(listOf(e)).left()
    }

    init {
        register(
            Modules.Attachment, AttachmentModule(config[Modules.Attachment.extensionBlacklist])
        )

        register(
            Modules.ConfirmParent,
            ConfirmParentModule(
                config[Modules.ConfirmParent.confirmationStatusWhitelist],
                config[Modules.ConfirmParent.targetConfirmationStatus],
                config[Modules.ConfirmParent.linkedThreshold]
            )
        )

        register(
            Modules.Crash,
            CrashModule(
                config[Modules.Crash.crashExtensions],
                config[Modules.Crash.duplicates],
                config[Modules.Crash.maxAttachmentAge],
                CrashReader(),
                config[Modules.Crash.duplicateMessage],
                config[Modules.Crash.moddedMessage]
            )
        )

        register(Modules.Empty, EmptyModule(config[Modules.Empty.message]))

        register(
            Modules.DuplicateMessage,
            DuplicateMessageModule(
                config[Modules.DuplicateMessage.message],
                config[Modules.DuplicateMessage.ticketMessages],
                config[Modules.DuplicateMessage.privateMessage],
                config[Modules.DuplicateMessage.resolutionMessages]
            )
        )

        register(Modules.FutureVersion, FutureVersionModule(config[Modules.FutureVersion.message]))

        register(Modules.HideImpostors, HideImpostorsModule())

        register(
            Modules.KeepPrivate, KeepPrivateModule(
                config[Modules.KeepPrivate.tag],
                config[Modules.KeepPrivate.message]
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
            Modules.Language,
            LanguageModule(
                config[Modules.Language.allowedLanguages],
                config[Modules.Language.lengthThreshold],
                ::getLanguage.partially1(config[Credentials.dandelionToken])
            )
        )

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
                config[Modules.ReopenAwaiting.keepARTag]
            )
        )

        register(Modules.ReplaceText, ReplaceTextModule())

        register(Modules.RevokeConfirmation, RevokeConfirmationModule())

        register(Modules.ResolveTrash, ResolveTrashModule())

        register(
            Modules.UpdateLinked,
            UpdateLinkedModule(config[Modules.UpdateLinked.updateInterval])
        ) { lastRun ->
            val now = Instant.now()
            val intervalStart = now.minus(config[Modules.UpdateLinked.updateInterval], ChronoUnit.HOURS)
            val intervalEnd = intervalStart.minusMillis(now.toEpochMilli() - lastRun.toEpochMilli())
            return@register "updated > ${lastRun.toEpochMilli()} OR (updated < ${intervalStart.toEpochMilli()}" +
                    " AND updated > ${intervalEnd.toEpochMilli()})"
        }
    }
}
