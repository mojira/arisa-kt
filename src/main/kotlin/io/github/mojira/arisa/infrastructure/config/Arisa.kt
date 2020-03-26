package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    val shadow by optional(false)
    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
    }

    object Issues : ConfigSpec() {
        val projects by optional(listOf("MC", "MCTEST", "MCPE", "MCAPI", "MCL", "MC", "MCE"))
        val url by optional("https://bugs.mojang.com/")
        val checkInterval by optional(10L)
    }

    object CustomFields : ConfigSpec() {
        val chkField by optional("customfield_10701")
        val confirmationField by optional("customfield_10500")
        val mojangPriorityField by optional("customfield_12200")
        val triagedTimeField by optional("customfield_12201")
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val whitelist by optional(listOf("MC", "MCTEST", "MCPE", "MCAPI", "MCL"))
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
                    "nova",
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
            val meqsTags by optional(listOf("MEQS_WAI", "MEQS_WONT_FIX"))
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
            val emptyMessage by optional("We are unable to diagnose your issue due to the lack of proper debug information." +
                    "Please review the [guidelines|http://help.mojang.com/customer/portal/articles/801354-writing-helpful-bug-reports-for-minecraft] before reporting issues.\r\n" +
                    "In case of a game crash, please also attach the crash log from" +
                    "{{[minecraft/crash-reports/crash-<DATE>-client.txt|http://hopper.minecraft.net/help/guides/finding-minecraft-data-folder/]}}."
            )
        }
    }
}
