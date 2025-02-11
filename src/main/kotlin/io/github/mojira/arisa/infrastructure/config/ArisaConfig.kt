package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    object Credentials : ConfigSpec() {
        // TODO: Remove username and password
        val username by required<String>()
        val password by required<String>()
        val email by required<String>()
        val apiToken by required<String>()
        val dandelionToken by optional<String?>(
            null,
            description = "Token for dandelion.eu"
        )
        val discordLogWebhook by optional<String?>(
            null,
            description = "Webhook to post log in a Discord channel"
        )
        val discordErrorLogWebhook by optional<String?>(
            null,
            description = "Webhook to post errors in a Discord channel"
        )
    }

    object Issues : ConfigSpec() {
        val projects by required<List<String>>(
            description = "The projects to operate on. Used for default whitelist of modules"
        )
        val resolutions by required<List<String>>(
            description = "The resolutions to operate on. Used for default whitelist of modules"
        )
        val url by required<String>(description = "The base url for the jira instance")
    }

    object CustomFields : ConfigSpec() {
        val linked by required<String>()
        val chkField by required<String>()
        val confirmationField by required<String>()
        val dungeonsPlatformField by required<String>()
        val legendsPlatformField by required<String>()
        val mojangPriorityField by required<String>()
        val triagedTimeField by required<String>()
        val platformField by required<String>()
    }

    object PrivateSecurityLevel : ConfigSpec() {
        val default by required<String>(
            description = "The default security id used by projects not defined in special."
        )
    }

    object Debug : ConfigSpec() {
        val enabledModules by optional<List<String>?>(
            null,
            description = "Disable all modules except for those in the list. " +
                "Entries must be module names (e.g. 'ReopenAwaiting'). " +
                "All modules are enabled if this is not specified."
        )

        val logOperationNotNeeded by optional(false)
        val logReturnedIssues by optional(false)
        val logQueryJql by optional(false)

        val ticketWhitelist by optional<List<String>?>(
            null,
            description = "Ignore all tickets except those mentioned here. " +
                "Entries must be valid Mojira ticket keys. " +
                "The whitelist is disabled if this is not specified."
        )

        val updateLastRun by optional(
            true,
            description = "Whether or not the lastRun file should be saved after each run. " +
                "Do not disable this unless you've enabled the ticket whitelist."
        )

        val ignoreOwnCommands by optional(
            true,
            description = "Whether to ignore commands from the bot user itself."
        )
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val enabled by optional(
                true,
                description = "Optional. Whether this module is enabled. " +
                    "Modules are enabled by default unless debug.enabledModules is defined."
            )
            val projects by optional<List<String>?>(
                null,
                description = "Optional. The projects this module should operate on. Default is arisa.issues.projects"
            )
            val resolutions by optional<List<String>?>(
                null,
                description = "Optional. The resolutions that should be considered for this module." +
                    " Default is arisa.issues.resolutions"
            )
            val excludedStatuses by optional(
                emptyList<String>(),
                description = "A list of statuses that are not considered for this module. Important for modules" +
                    " that resolve or update, as those transitions do not exist for Postponed."
            )
        }

        object AffectedVersionMessage : ModuleConfigSpec() {
            val versionIdMessageMap by optional<Map<String, String>>(
                description = "Map from Jira version ID to message key of the message to add when that version has " +
                    "been selected as affected version.",
                default = emptyMap()
            )
        }

        object Attachment : ModuleConfigSpec() {
            val extensionBlacklist by required<List<String>>(
                description = "The extensions (including leading dots) that should be removed from issues. " +
                    "Default is no extensions."
            )
            val comment by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
        }

        object DuplicateMessage : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted under duplicate tickets."
            )
            val forwardMessage by required<String>(
                description = "The key of the message that is posted under duplicate tickets that are " +
                    "newer than the current one."
            )
            val ticketMessages by required<Map<String, String>>(
                description = "A map from ticket keys to keys of messages that are posted for specific parents"
            )
            val privateMessage by required<String>(
                description = "The key of the message that is posted when the parent is private."
            )
            val resolutionMessages by required<Map<String, String>>(
                description = "A map from resolution names to keys of messages that are posted when the parents were" +
                    " resolved as specific resolutions"
            )
            val commentDelayMinutes by required<Long>(
                description = "Delay in which the module should add the comment in minutes"
            )
            val preventMessageTags by required<List<String>>(
                description = "A list of tags used to indicate that Arisa should not comment the duplicate message"
            )
        }

        object Piracy : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
            val piracySignatures by required<List<String>>(
                description = "Signatures that indicate a pirated version of Minecraft. Default is no signatures."
            )
        }

        object Privacy : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
            val commentNote by optional(
                "",
                description = "The text which will be appended at the comments that are restricted by this module."
            )
            val allowedEmailRegexes by optional<List<String>>(
                default = emptyList(),
                description = "Regex patterns matching allowed email addresses, that is, email addresses which may" +
                    " be public. The patterns must match the complete email address; partial matches won't have any" +
                    " effect."
            )
            val sensitiveTextRegexes by optional<List<String>>(
                default = emptyList(),
                description = "Regex patterns matching sensitive text. The patterns do not have to match the" +
                    " complete text; partial matches will be detected as well."
            )
            val sensitiveFileNameRegexes by optional<List<String>>(
                default = emptyList(),
                description = "Regex patterns matching names of attachment files containing sensitive information." +
                    " The patterns must match the complete file name; partial matches won't have any effect."
            )
        }

        object Language : ModuleConfigSpec() {
            val apiQuotaWarningThreshold by optional<Double?>(
                description = "When the number of remaining API quota units becomes equal to or lower than this" +
                    " value a warning will be logged.",
                default = null
            )
            val allowedLanguages by required<List<String>>(
                description = "Codes of languages that can be used."
            )
            val message by required<String>(
                description = "Key of message in helper-messages."
            )
            val lengthThreshold by required<Int>(
                description = "The minimum string length that the combined summary and description text must exceed" +
                    " before they can be detected by this module (inclusive)."
            )
        }

        object RemoveTriagedMeqs : ModuleConfigSpec() {
            val meqsTags by required<List<String>>(
                description = "List of tags that should be removed by the bot when an issue is triaged."
            )
            val removalReason by required<String>(
                description = "Reason Arisa should add to the edited comment for removing the tag. Default is empty."
            )
        }

        object FutureVersion : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
            val panel by required<String>(
                description = "The key of the message that is posted when there are more versions."
            )
        }

        object CHK : ModuleConfigSpec()

        object ConfirmParent : ModuleConfigSpec() {
            val confirmationStatusWhitelist by required<List<String>>(
                description = "List of confirmation status that can be replaced by the target status if Linked is" +
                    " greater than or equal to the threshold."
            )
            val targetConfirmationStatus by required<String>(
                description = "The target confirmation status for tickets whose Linked is greater than or equal" +
                    " to the threshold."
            )
            val linkedThreshold by required<Double>(
                description = "The threshold of the Linked field for the ticket to be confirmed (inclusive)."
            )
        }

        object MultiplePlatforms : ModuleConfigSpec() {
            val platformWhitelist by required<List<String>>(
                description = "List of platforms that can be replaced by the target platform if they are" +
                    " different than the replacement."
            )
            val dungeonsPlatformWhitelist by required<List<String>>(
                description = "List of Dungeons platforms that can be replaced by the target platform if they are" +
                    " different than the replacement."
            )
            val legendsPlatformWhitelist by required<List<String>>(
                description = "List of Legends platforms that can be replaced by the target platform if they are" +
                    " different than the replacement."
            )
            val targetPlatform by required<String>(
                description = "The target platform for tickets with more than one platform"
            )
            val transferredPlatformBlacklist by required<List<String>>(
                description = "List of platforms that do not contribute to having multiple platforms"
            )
            val keepPlatformTag by required<String>(
                description = "The meqs tag that when placed in the comments will prevent the" +
                    " plaform from being changed. Must be the same as KeepPlatform"
            )
        }

        object KeepPlatform : ModuleConfigSpec() {
            val keepPlatformTag by required<String>(
                description = "The meqs tag that when placed in the comments will prevent the" +
                    " plaform from being changed. Must be the same as MultiplePlatforms"
            )
        }

        object ReopenAwaiting : ModuleConfigSpec() {
            val blacklistedRoles by required<List<String>>(
                description = "Comments that were posted by someone who is member of this role should be ignored."
            )
            val blacklistedVisibilities by required<List<String>>(
                description = "Comments that are restricted to one of these roles should be ignored"
            )
            val keepARTag by required<String>(
                description = "A tag used to indicate that Arisa should keep the ticket Awaiting Response"
            )
            val softARDays by required<Long>(
                description = "The ticket can also be reopened by comments posted by people other than the reporter " +
                    "within the specific days after it was resolved. After the time has passed, only the " +
                    "reporter can reopen the ticket."
            )
            val onlyOPTag by required<String>(
                description = "a tag used to indicate that only the reporter should be allowed to reopen the ticket"
            )
            val message by required<String>(
                description = "The key of the message that is posted when the ticket is updated but will not be " +
                    "reopened by Arisa, e.g. the ticket has a keep AR tag, or the ticket is too old and is not " +
                    "updated by the reporter."
            )
        }

        object RemoveNonStaffTags : ModuleConfigSpec() {
            val removalReason by required<String>(
                description = "Reason Arisa should add to the edited comment for" +
                    " removing the tag. Default is no reason."
            )
            val removablePrefixes by required<List<String>>(
                description = "The prefixes of tags that should be removed by the module."
            )
        }

        object Incomplete : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
        }

        object Crash : ModuleConfigSpec() {
            val crashExtensions by required<List<String>>(
                description = "File extensions that should be checked for crash reports."
            )
            val duplicateMessage by required<String>(
                description = "The key of the message to be sent when resolving a duplicate."
            )
            val moddedMessage by required<String>(
                description = "The key of the message to be sent when resolving a duplicate."
            )
            val minecraftCrashDuplicates by optional<List<MinecraftCrashDupeConfig>>(
                description = "List of Minecraft crash details that are resolved as duplicate for a specific ticket" +
                    " key.",
                default = emptyList()
            )
            val jvmCrashDuplicates by optional<List<JvmCrashDupeConfig>>(
                description = "List of JVM crash details that are resolved as duplicate for a specific ticket key.",
                default = emptyList()
            )
        }

        object RevokeConfirmation : ModuleConfigSpec()

        object RevokePriority : ModuleConfigSpec()

        object KeepPrivate : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
            val tag by optional<String?>(null)
        }

        object PrivateDuplicate : ModuleConfigSpec() {
            val tag by optional<String?>(null)
        }

        object HideImpostors : ModuleConfigSpec()

        object RemoveSpam : ModuleConfigSpec() {
            val patterns by required<List<SpamPatternConfig>>(
                description = "Patterns that indicate that a comment is spam"
            )
        }

        object ResolveTrash : ModuleConfigSpec()

        object UpdateLinked : ModuleConfigSpec() {
            val updateIntervalHours by required<Long>(
                description = "Interval in which the module should update the Linked field in hours"
            )
        }

        object TransferVersions : ModuleConfigSpec() {
            val notTransferredVersionIds by optional<List<String>>(
                description = "List of Jira version IDs which should not be transferred.",
                default = emptyList()
            )
        }

        object TransferLinks : ModuleConfigSpec()

        object ReplaceText : ModuleConfigSpec()

        object RemoveIdenticalLink : ModuleConfigSpec()

        object RemoveVersion : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
        }

        object Command : ModuleConfigSpec() {
            val commandPrefix by required<String>(
                description = "The prefix for all arisa commands. It should not contain the joining underline."
            )
        }

        object Thumbnail : ModuleConfigSpec() {
            private const val DEFAULT_MAX_READ_BYTES: Long = 1024 * 5 // 5 KiB

            val maxImageWidth by required<Int>(
                description = "Maximum width (in pixels) an image may have; for larger images a thumbnail will be used"
            )
            val maxImageHeight by required<Int>(
                description = "Maximum height (in pixels) an image may have; for larger images a thumbnail will be used"
            )
            val maxImageReadBytes by optional(
                description = "Maximum number of bytes which may be read from an image. If the module tries to read " +
                    "more bytes when parsing the image it will abort processing the image. Mostly intended to " +
                    "prevent DoS attacks.",
                default = DEFAULT_MAX_READ_BYTES
            )
            val maxImagesCount by optional(
                description = "Maximum number of embedded images to process per comment respectively issue " +
                    "description. Used to protect against malicious comments / issues containing hundreds of " +
                    "embedded images.",
                default = 10
            )
        }

        object RemoveBotComment : ModuleConfigSpec() {
            val removalTag by required<String>(
                description = "The key to search for in a bot comment to trigger the removal of the comment"
            )
        }

        object Shadowban : ModuleConfigSpec()
    }
}

data class MinecraftCrashDupeConfig(
    val exceptionRegex: String,
    /** Issue key of the duplicated issue */
    val duplicates: String
)

data class JvmCrashDupeConfig(
    val libraryNameRegex: String,
    /** Issue key of the duplicated issue */
    val duplicates: String
)

data class SpamPatternConfig(
    val pattern: String,
    val threshold: Int
) {
    val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
}
