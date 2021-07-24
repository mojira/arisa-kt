package io.github.mojira.arisa.registry

import com.uchuhimo.konf.Config
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.infrastructure.LanguageDetectionApi
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.AttachmentModule
import io.github.mojira.arisa.modules.CHKModule
import io.github.mojira.arisa.modules.CommandModule
import io.github.mojira.arisa.modules.ConfirmParentModule
import io.github.mojira.arisa.modules.CrashModule
import io.github.mojira.arisa.modules.EmptyModule
import io.github.mojira.arisa.modules.FutureVersionModule
import io.github.mojira.arisa.modules.HideImpostorsModule
import io.github.mojira.arisa.modules.KeepPlatformModule
import io.github.mojira.arisa.modules.KeepPrivateModule
import io.github.mojira.arisa.modules.LanguageModule
import io.github.mojira.arisa.modules.MultiplePlatformsModule
import io.github.mojira.arisa.modules.PiracyModule
import io.github.mojira.arisa.modules.PrivacyModule
import io.github.mojira.arisa.modules.PrivateDuplicateModule
import io.github.mojira.arisa.modules.RemoveIdenticalLinkModule
import io.github.mojira.arisa.modules.RemoveNonStaffTagsModule
import io.github.mojira.arisa.modules.RemoveSpamModule
import io.github.mojira.arisa.modules.RemoveTriagedMeqsModule
import io.github.mojira.arisa.modules.RemoveVersionModule
import io.github.mojira.arisa.modules.ReopenAwaitingModule
import io.github.mojira.arisa.modules.ReplaceTextModule
import io.github.mojira.arisa.modules.ResolveTrashModule
import io.github.mojira.arisa.modules.RevokeConfirmationModule
import io.github.mojira.arisa.modules.ThumbnailModule
import io.github.mojira.arisa.modules.TransferLinksModule
import io.github.mojira.arisa.modules.TransferVersionsModule

/**
 * This class is the registry for modules that get executed immediately after a ticket has been updated.
 */
class InstantModuleRegistry(config: Config) : ModuleRegistry(config) {
    override fun getJql(timeframe: ExecutionTimeframe): String {
        return timeframe.getFreshlyUpdatedJql()
    }

    init {
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

        register(
            Arisa.Modules.Crash,
            CrashModule(
                config[Arisa.Modules.Crash.crashExtensions],
                config[Arisa.Modules.Crash.duplicates],
                CrashReader(),
                config[Arisa.Modules.Crash.duplicateMessage],
                config[Arisa.Modules.Crash.moddedMessage],
                config[Arisa.Credentials.username]
            )
        )

        register(Arisa.Modules.Empty, EmptyModule(config[Arisa.Modules.Empty.message]))

        register(Arisa.Modules.HideImpostors, HideImpostorsModule())

        register(
            Arisa.Modules.RemoveSpam,
            RemoveSpamModule(
                config[Arisa.Modules.RemoveSpam.patterns]
            )
        )

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

        register(Arisa.Modules.TransferVersions, TransferVersionsModule())

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
                config[Arisa.Modules.Privacy.allowedEmailRegex].map(String::toRegex),
                config[Arisa.Modules.Privacy.sensitiveFileNames]
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
                config[Arisa.Modules.ReopenAwaiting.blacklistedRoles],
                config[Arisa.Modules.ReopenAwaiting.blacklistedVisibilities],
                config[Arisa.Modules.ReopenAwaiting.softARDays],
                config[Arisa.Modules.ReopenAwaiting.keepARTag],
                config[Arisa.Modules.ReopenAwaiting.onlyOPTag],
                config[Arisa.Modules.ReopenAwaiting.message]
            )
        )

        register(Arisa.Modules.ReplaceText, ReplaceTextModule())

        register(Arisa.Modules.RevokeConfirmation, RevokeConfirmationModule())

        register(Arisa.Modules.ResolveTrash, ResolveTrashModule())

        register(
            Arisa.Modules.FutureVersion,
            FutureVersionModule(
                config[Arisa.Modules.FutureVersion.message],
                config[Arisa.Modules.FutureVersion.panel],
                config[Arisa.Modules.FutureVersion.resolveAsInvalidMessages]
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
                config[Arisa.Credentials.username]
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
    }
}
