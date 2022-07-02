package io.github.mojira.arisa.registry

import com.uchuhimo.konf.Config
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.infrastructure.LanguageDetectionApi
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.AffectedVersionMessageModule
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CommandModule
import io.github.mojira.arisa.modules.ConfirmParentModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.IncompleteModule
import io.github.mojira.arisa.modules.KeepPlatformModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.LanguageModule
import io.github.mojira.arisa.modules.MultiplePlatformsModule
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.PrivateDuplicateModule
import io.github.mojira.arisa.modules.RemoveBotCommentModule
import io.github.mojira.arisa.modules.RemoveIdenticalLinkModule
import io.github.mojira.arisa.modules.RemoveNonStaffTagsModule
import io.github.mojira.arisa.modules.RemoveSpamModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.RemoveVersionModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReplaceTextModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.RevokePriorityModule
import io.github.mojira.arisa.modules.ShadowbanModule
import io.github.mojira.arisa.modules.TransferLinksModule
import io.github.mojira.arisa.modules.TransferVersionsModule
import io.github.mojira.arisa.modules.privacy.AccessTokenRedactor
import io.github.mojira.arisa.modules.privacy.PrivacyModule
import io.github.mojira.arisa.modules.thumbnail.ThumbnailModule

/**
 * This class is the registry for modules that get executed immediately after a ticket has been updated.
 */
class InstantModuleRegistry(config: Config) : ModuleRegistry(config) {
    override fun getJql(timeframe: ExecutionTimeframe): String {
        return timeframe.getFreshlyUpdatedJql()
    }

    init {
        register(
            Arisa.Modules.Shadowban,
            ShadowbanModule()
        )

        register(
            Arisa.Modules.RemoveSpam,
            RemoveSpamModule(
                config[Arisa.Modules.RemoveSpam.patterns]
            )
        )

        register(
            Arisa.Modules.AffectedVersionMessage,
            AffectedVersionMessageModule(
                config[Arisa.Modules.AffectedVersionMessage.versionIdMessageMap]
            )
        )

        register(
            Arisa.Modules.Attachment,
            AttachmentModule(
                config[Arisa.Modules.Attachment.extensionBlacklist],
                config[Arisa.Modules.Attachment.comment]
            )
        )

        register(Arisa.Modules.CHK, CHKModule())

        register(
            Arisa.Modules.ConfirmParent,
            ConfirmParentModule(
                config[Arisa.Modules.ConfirmParent.confirmationStatusWhitelist],
                config[Arisa.Modules.ConfirmParent.targetConfirmationStatus],
                config[Arisa.Modules.ConfirmParent.linkedThreshold]
            )
        )

        register(
            Arisa.Modules.MultiplePlatforms,
            MultiplePlatformsModule(
                config[Arisa.Modules.MultiplePlatforms.dungeonsPlatformWhitelist],
                config[Arisa.Modules.MultiplePlatforms.platformWhitelist],
                config[Arisa.Modules.MultiplePlatforms.targetPlatform],
                config[Arisa.Modules.MultiplePlatforms.transferredPlatformBlacklist],
                config[Arisa.Modules.MultiplePlatforms.keepPlatformTag]
            )
        )

        register(
            Arisa.Modules.KeepPlatform,
            KeepPlatformModule(
                config[Arisa.Modules.KeepPlatform.keepPlatformTag]
            )
        )

        val attachmentUtils = AttachmentUtils(
            config[Arisa.Modules.Crash.crashExtensions],
            CrashReader()
        )
        register(
            Arisa.Modules.Crash,
            CrashModule(
                attachmentUtils,
                config[Arisa.Modules.Crash.minecraftCrashDuplicates],
                config[Arisa.Modules.Crash.jvmCrashDuplicates],
                config[Arisa.Modules.Crash.duplicateMessage],
                config[Arisa.Modules.Crash.moddedMessage]
            )
        )

        register(Arisa.Modules.Incomplete, IncompleteModule(config[Arisa.Modules.Incomplete.message]))

        register(Arisa.Modules.HideImpostors, HideImpostorsModule())

        register(
            Arisa.Modules.KeepPrivate,
            KeepPrivateModule(
                config[Arisa.Modules.KeepPrivate.tag],
                config[Arisa.Modules.KeepPrivate.message]
            )
        )

        register(
            Arisa.Modules.PrivateDuplicate,
            PrivateDuplicateModule(
                config[Arisa.Modules.PrivateDuplicate.tag]
            )
        )

        register(
            Arisa.Modules.TransferVersions,
            TransferVersionsModule(
                config[Arisa.Modules.TransferVersions.notTransferredVersionIds]
            )
        )

        register(
            Arisa.Modules.TransferLinks,
            TransferLinksModule()
        )

        register(
            Arisa.Modules.Piracy,
            PiracyModule(
                config[Arisa.Modules.Piracy.piracySignatures],
                config[Arisa.Modules.Piracy.message]
            )
        )

        register(
            Arisa.Modules.Privacy,
            PrivacyModule(
                config[Arisa.Modules.Privacy.message],
                config[Arisa.Modules.Privacy.commentNote],
                config[Arisa.Modules.Privacy.allowedEmailRegexes].map(String::toRegex),
                config[Arisa.Modules.Privacy.sensitiveTextRegexes].map(String::toRegex),
                AccessTokenRedactor,
                config[Arisa.Modules.Privacy.sensitiveFileNameRegexes].map(String::toRegex)
            )
        )

        register(
            Arisa.Modules.Language,
            LanguageModule(
                config[Arisa.Modules.Language.allowedLanguages],
                config[Arisa.Modules.Language.lengthThreshold],
                LanguageDetectionApi(
                    config[Arisa.Credentials.dandelionToken],
                    config[Arisa.Modules.Language.apiQuotaWarningThreshold]
                )::getLanguage
            )
        )

        register(Arisa.Modules.RemoveIdenticalLink, RemoveIdenticalLinkModule())

        register(
            Arisa.Modules.RemoveNonStaffTags,
            RemoveNonStaffTagsModule(
                config[Arisa.Modules.RemoveNonStaffTags.removalReason],
                config[Arisa.Modules.RemoveNonStaffTags.removablePrefixes]
            )
        )

        register(
            Arisa.Modules.RemoveTriagedMeqs,
            RemoveTriagedMeqsModule(
                config[Arisa.Modules.RemoveTriagedMeqs.meqsTags],
                config[Arisa.Modules.RemoveTriagedMeqs.removalReason]
            )
        )

        register(
            Arisa.Modules.ReopenAwaiting,
            ReopenAwaitingModule(
                config[Arisa.Modules.ReopenAwaiting.blacklistedRoles].toSetNoDuplicates(),
                config[Arisa.Modules.ReopenAwaiting.blacklistedVisibilities].toSetNoDuplicates(),
                config[Arisa.Modules.ReopenAwaiting.softARDays],
                config[Arisa.Modules.ReopenAwaiting.keepARTag],
                config[Arisa.Modules.ReopenAwaiting.onlyOPTag],
                config[Arisa.Modules.ReopenAwaiting.message]
            )
        )

        register(Arisa.Modules.ReplaceText, ReplaceTextModule())

        register(Arisa.Modules.RevokeConfirmation, RevokeConfirmationModule())

        register(Arisa.Modules.RevokePriority, RevokePriorityModule())

        register(Arisa.Modules.ResolveTrash, ResolveTrashModule())

        register(
            Arisa.Modules.FutureVersion,
            FutureVersionModule(
                config[Arisa.Modules.FutureVersion.message],
                config[Arisa.Modules.FutureVersion.panel]
            )
        )

        register(
            Arisa.Modules.RemoveVersion,
            RemoveVersionModule(
                config[Arisa.Modules.RemoveVersion.message]
            )
        )

        register(
            Arisa.Modules.Command,
            CommandModule(
                config[Arisa.Modules.Command.commandPrefix],
                config[Arisa.Credentials.username],
                config[Arisa.Debug.ignoreOwnCommands],
                attachmentUtils
            )
        )

        register(
            Arisa.Modules.Thumbnail,
            ThumbnailModule(
                maxImageWidth = config[Arisa.Modules.Thumbnail.maxImageWidth],
                maxImageHeight = config[Arisa.Modules.Thumbnail.maxImageHeight],
                maxImageReadBytes = config[Arisa.Modules.Thumbnail.maxImageReadBytes],
                maxImagesCount = config[Arisa.Modules.Thumbnail.maxImagesCount]
            )
        )

        register(
            Arisa.Modules.RemoveBotComment,
            RemoveBotCommentModule(
                config[Arisa.Credentials.username],
                config[Arisa.Modules.RemoveBotComment.removalTag]
            )
        )
    }

    private fun <T> List<T>.toSetNoDuplicates(): Set<T> {
        val result = toMutableSet()
        if (result.size != size) {
            val duplicates = mutableSetOf<T>()
            for (element in this) {
                // If removal fails it is a duplicate element because it has already been removed
                if (!result.remove(element)) {
                    duplicates.add(element)
                }
            }
            throw IllegalArgumentException("Contains these duplicate elements: $duplicates")
        }

        return result
    }
}
