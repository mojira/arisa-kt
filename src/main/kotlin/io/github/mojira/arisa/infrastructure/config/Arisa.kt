package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    val logOperationNotNeeded by optional(false)

    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
        val dandelionToken by Modules.Language.required<String>(description = "Token for dandelion.eu")
    }

    object Issues : ConfigSpec() {
        val projects by required<List<String>>(description = "The projects to operate on. Used for default whitelist of modules")
        val url by required<String>(description = "The base url for the jira instance")
        val checkInterval by required<Long>(description = "The interval in which all issues are checked")
    }

    object CustomFields : ConfigSpec() {
        val linked by required<String>()
        val chkField by required<String>()
        val confirmationField by required<String>()
        val mojangPriorityField by required<String>()
        val triagedTimeField by required<String>()
    }

    object PrivateSecurityLevel : ConfigSpec() {
        val default by required<String>(description = "The default security id used by projects not defined in special.")
        val special by optional<Map<String, String>>(emptyMap(), description = "Some projects define their own security level. These projects need to be defined here with their own ID.. Default is all projects use the default ID")
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val whitelist by optional<List<String>?>(null, description = "Optional. The projects this module should operate on. Default is arisa.issues.projects")
            val resolutions by optional(listOf("unresolved"), description = "Optional. The resolutions that should be considered for this module. Default is unresolved.")
            val jql by optional("updated > %s", description = "Optional. Jql query that should be used to fetch issues from this module %s will be replaced by the last time all queries successfully ran. Default is updated since last run.")
        }

        object Attachment : ModuleConfigSpec() {
            val extensionBlacklist by optional(emptyList<String>(), description = "The extensions that should be removed on issues. Default is no extensions.")
        }

        object Piracy : ModuleConfigSpec() {
            val message by optional("", description = "The message that is posted when this module succeeds.")
            val piracySignatures by optional(emptyList<String>(), description = "Signatures that indicate a pirated version of Minecraft. Default is no signatures.")
        }

        object Language : ModuleConfigSpec() {
            val messages by optional(emptyMap<String, String>(), description = "Translated messages for various languages. Use lowercase ISO 639-1 as keys. Default is no translated messages.")
            val defaultMessage by optional("", description = "The message that is posted when this module succeeds.")
            val messageFormat by optional("%s\n----\n%s", description = "The message format to be used if the translated message is present. First argument is translated message, second is default message.")
        }

        object RemoveTriagedMeqs : ModuleConfigSpec() {
            val meqsTags by optional(emptyList<String>(), description = "List of tags that should be removed by the bot when an issue is triaged.")
            val removalReason by required<String>(description = "Reason Arisa should add to the edited comment for removing the tag. Default is no reason.")
        }

        object FutureVersion : ModuleConfigSpec() {
            val message by optional("", description = "The message that is posted when this module succeeds.")
        }

        object CHK : ModuleConfigSpec()

        object ReopenAwaiting : ModuleConfigSpec()

        object RemoveNonStaffMeqs : ModuleConfigSpec() {
            val removalReason by required<String>(description = "Reason Arisa should add to the edited comment for removing the tag. Default is no reason.")
        }

        object Empty : ModuleConfigSpec() {
            val message by optional("", description = "The message that is posted when this module succeeds.")
        }

        object Crash : ModuleConfigSpec() {
            val maxAttachmentAge by optional(0, description = "Max age in days the attachment can have to be considered")
            val crashExtensions by optional(emptyList<String>(), description = "File extensions that should be checked for crash reports.")
            val duplicateMessage by optional("", description = "Message to be send when resolving a duplicate. {DUPLICATE} will be replaced by the ticket key")
            val moddedMessage by optional("", description = "Message to be send when resolving a duplicate. %s will be replaced by the ticket key")
            val duplicates by optional(emptyList<CrashDupeConfig>(), description = "List of exception details that are resolved as duplicates for a specific ticket key.")
        }

        object RevokeConfirmation : ModuleConfigSpec()

        object KeepPrivate : ModuleConfigSpec() {
            val message by optional("", description = "The message that is posted when this module succeeds.")
            val tag by optional<String?>(null)
        }

        object HideImpostors : ModuleConfigSpec()

        object ResolveTrash : ModuleConfigSpec()

        object UpdateLinked : ModuleConfigSpec()
    }
}

data class CrashDupeConfig(
    val type: String,
    val exceptionRegex: String,
    val duplicates: String
)
