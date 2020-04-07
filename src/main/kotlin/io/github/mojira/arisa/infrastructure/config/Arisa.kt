package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    val shadow by optional(false)

    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
    }

    object Issues : ConfigSpec() {
        val projects by optional(listOf("MC", "MCTEST", "MCPE", "MCAPI", "MCL", "MCD", "MCE", "BDS"))
        val url by optional("https://bugs.mojang.com/")
        val checkInterval by optional(10L)
    }

    object CustomFields : ConfigSpec() {
        val chkField by optional("customfield_10701")
        val confirmationField by optional("customfield_10500")
        val mojangPriorityField by optional("customfield_12200")
        val triagedTimeField by optional("customfield_12201")
    }

    object PrivateSecurityLevel : ConfigSpec() {
        val default by optional("10318")
        val special by optional(mapOf(
            Pair("MCL", "10502"),
            Pair("MCAPI", "10313")
        ))
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val whitelist by optional(listOf("MC", "MCTEST", "MCPE", "MCAPI", "MCL", "MCD", "MCE", "BDS"))
        }

        object Attachment : ModuleConfigSpec() {
            val extensionBlacklist by optional(listOf("jar", "exe", "com", "bat", "msi", "run", "lnk", "dmg"))
        }

        object Piracy : ModuleConfigSpec() {
            val piracyMessage by optional(
                "You are currently using a *non-authorized* version of Minecraft." +
                        " If you wish to purchase the full game, please visit the [Minecraft Store|https://minecraft.net/store].\r\n" +
                        "We will not provide support for pirated versions of the game, these versions are modified and may contain malware."
            )
            val piracySignatures by optional(
                listOf(
                    "Minecraft Launcher null",
                    "Bootstrap 0",
                    "Launcher: 1.0.10  (bootstrap 4)",
                    "Launcher: 1.0.10  (bootstrap 5)",
                    "Launcher 3.0.0",
                    "Launcher: 3.1.0",
                    "Launcher: 3.1.1",
                    "Launcher: 3.1.4",
                    "1.0.8",
                    "uuid sessionId",
                    "auth_access_token",
                    "windows-\${arch}",
                    "keicraft",
                    "keinett",
                    "nodus",
                    "iridium",
                    "mcdonalds",
                    "uranium",
                    "divinity",
                    "gemini",
                    "mineshafter",
                    "Team-NeO",
                    "DarkLBP",
                    "Launcher X",
                    "PHVL",
                    "Pre-Launcher v6",
                    "LauncherFEnix",
                    "TLauncher"
                )
            )
        }

        object RemoveTriagedMeqs : ModuleConfigSpec() {
            val meqsTags by optional(listOf("MEQS_WAI", "MEQS_WONTFIX"))
        }

        object FutureVersion : ModuleConfigSpec() {
            val futureVersionMessage by optional(
                "Please *do not* mark _unreleased versions_ as affected.\r\nYou don't have access to them yet."
            )
        }

        object CHK : ModuleConfigSpec()

        object ReopenAwaiting : ModuleConfigSpec()

        object RemoveNonStaffMeqs : ModuleConfigSpec()

        object Empty : ModuleConfigSpec() {
            val emptyMessage by Crash.optional(
                "We are unable to diagnose your issue due to the lack of proper debug information. " +
                        "Please review the [guidelines|http://help.mojang.com/customer/portal/articles/801354-writing-helpful-bug-reports-for-minecraft] before reporting issues.\r\n" +
                        "In case of a game crash, please also attach the crash log from " +
                        "{{[minecraft/crash-reports/crash-<DATE>-client.txt|http://hopper.minecraft.net/help/guides/finding-minecraft-data-folder/]}}."
            )
        }

        object Crash : ModuleConfigSpec() {
            val maxAttachmentAge by optional(30)
            val crashExtensions by optional(listOf("txt", "log"))
            val duplicateMessage by optional(
                "Duplicate of {DUPLICATE} -- " +
                        "If you have not, please use the [search function|https://bugs.mojang.com/issues/] in the future, " +
                        "to see if your bug has already been submitted.\r\n" +
                        "For technical support, please use the " +
                        "[Mojang Support Center|http://help.mojang.com/customer/portal/articles/364794-where-can-i-find-more-help-and-technical-support-]."
            )
            val moddedMessage by optional(
                "This ticket is _invalid_ as it relates to a modified or third-party client, server, or launcher.\r\n" +
                        "* Any non-standard client/server/launcher build needs to be taken up with the appropriate team, not Mojang.\r\n" +
                        "* Any plugin issues need to be addressed to the creator of the plugin or resource pack.\r\n" +
                        "* This site is for addressing issues related to the *base unmodded Minecraft*; " +
                        "any modded system _invalidates_ your ticket, unless the behavior can be reproduced without mods.\r\n* " +
                        "Additionally, if you have problems on large-scale modded servers, please report it to their site. It's probably not a bug in Minecraft."
            )

            val duplicates by optional(
                listOf(
                    CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297"),
                    CrashDupeConfig("minecraft", "No OpenGL context found in the current thread", "MC-297"),
                    CrashDupeConfig("minecraft", "Could not create context", "MC-297"),
                    CrashDupeConfig("minecraft", "WGL: The driver does not appear to support OpenGL", "MC-128302"),
                    CrashDupeConfig("minecraft", "failed to create a child event loop", "MC-34749"),
                    CrashDupeConfig("minecraft", "Failed to check session lock, aborting", "MC-10167"),
                    CrashDupeConfig("minecraft", "Maybe try a lowerresolution texturepack", "MC-29565"),
                    CrashDupeConfig("minecraft", "java\\.lang\\.OutOfMemoryError\\: Java heap space", "MC-12949"),
                    CrashDupeConfig("minecraft", "try a lowerresolution", "MC-29565"),
                    CrashDupeConfig("java", "ig[0-9]{1,2}icd[0-9]{2}\\.dll", "MC-32606")
                )
            )
        }

        object KeepPrivate : ModuleConfigSpec() {
            val tag by optional("MEQS_KEEP_PRIVATE")
            val keepPrivateMessage by optional("Please *do not* remove the _security level_ from issues containing private information or describing exploits.")
        }
    }
}

data class CrashDupeConfig(
    val type: String,
    val exceptionDesc: String,
    val duplicates: String
)
