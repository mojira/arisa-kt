package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    val logOperationNotNeeded by optional(false)

    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
        val dandelionToken by required<String>(description = "Token for dandelion.eu")
    }

    object Issues : ConfigSpec() {
        val projects by required<List<String>>(
            description = "The projects to operate on. Used for default whitelist of modules"
        )
        val url by required<String>(description = "The base url for the jira instance")
        val checkIntervalSeconds by required<Long>(description = "The interval in which all issues are checked")
    }

    object CustomFields : ConfigSpec() {
        val linked by required<String>()
        val chkField by required<String>()
        val confirmationField by required<String>()
        val mojangPriorityField by required<String>()
        val triagedTimeField by required<String>()
    }

    object PrivateSecurityLevel : ConfigSpec() {
        val default by required<String>(
            description = "The default security id used by projects not defined in special."
        )
        val special by required<Map<String, String>>(
            description = "Some projects define their own security level. These projects need to be defined here with" +
                    " their own ID.. Default is all projects use the default ID"
        )
    }

    object HelperMessages : ConfigSpec() {
        val updateIntervalSeconds by required<Long>(
            description = "The interval in which the messages.json file is updated"
        )
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val only by optional(
                false,
                description = "Optional. If set to true, only this module will be executed."
            )
            val whitelist by optional<List<String>?>(
                null,
                description = "Optional. The projects this module should operate on. Default is arisa.issues.projects"
            )
            val resolutions by optional(
                listOf("unresolved"),
                description = "Optional. The resolutions that should be considered for this module." +
                        " Default is unresolved."
            )
            val excludedStatuses by optional(
                emptyList<String>(),
                description = "A list of statuses that are not considered for this module. Important for modules" +
                        " that resolve or update, as those transitions do not exist for Postponed."
            )
        }

        object Attachment : ModuleConfigSpec() {
            val extensionBlacklist by required<List<String>>(
                description = "The extensions that should be removed on issues. Default is no extensions."
            )
            val comment by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
        }

        object DuplicateMessage : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted under duplicate tickets."
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
        }

        object Language : ModuleConfigSpec() {
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
                    "within the specific days after it was resolved. After the time has passed, only the reporter " +
                    "can reopen the ticket."
            )
            val message by required<String>(
                description = "The key of the message that is posted when the ticket is updated but will not be " +
                        "reopened by Arisa, e.g. the ticket has a keep AR tag, or the ticket is too old and is not " +
                        "updated by the reporter."
            )
        }

        object RemoveNonStaffMeqs : ModuleConfigSpec() {
            val removalReason by required<String>(
                description = "Reason Arisa should add to the edited comment for" +
                        " removing the tag. Default is no reason."
            )
        }

        object Empty : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
        }

        object Crash : ModuleConfigSpec() {
            val maxAttachmentAge by required<Int>(
                description = "Max age in days the attachment can have to be considered"
            )
            val crashExtensions by required<List<String>>(
                description = "File extensions that should be checked for crash reports."
            )
            val duplicateMessage by required<String>(
                description = "The key of the message to be sent when resolving a duplicate."
            )
            val moddedMessage by required<String>(
                description = "The key of the message to be sent when resolving a duplicate."
            )
            val duplicates by required<List<CrashDupeConfig>>(
                description = "List of exception details that are resolved as duplicates for a specific ticket key."
            )
        }

        object RevokeConfirmation : ModuleConfigSpec()

        object KeepPrivate : ModuleConfigSpec() {
            val message by required<String>(
                description = "The key of the message that is posted when this module succeeds."
            )
            val tag by optional<String?>(null)
        }

        object HideImpostors : ModuleConfigSpec()

        object ResolveTrash : ModuleConfigSpec()

        object UpdateLinked : ModuleConfigSpec() {
            val updateIntervalHours by required<Long>(
                description = "Interval in which the module should update the Linked field in hours"
            )
        }

        object TransferVersions : ModuleConfigSpec()

        object TransferLinks : ModuleConfigSpec()

        object ReplaceText : ModuleConfigSpec()

        object RemoveIdenticalLink : ModuleConfigSpec()

        object Command : ModuleConfigSpec()
    }
}

data class CrashDupeConfig(
    val type: String,
    val exceptionRegex: String,
    val duplicates: String
)
