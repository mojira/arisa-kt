package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
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
                """
                *Thank you for your report!*
                However, this issue is {color:#FF5722}*Invalid*{color}.
                
                You are currently using a *non-authorized* version of Minecraft. If you wish to purchase the full game, please visit the [Minecraft Store|https://www.minecraft.net/store/minecraft-java-edition].
                We will not provide support for pirated versions of the game, these versions are modified and may contain malware.
                
                *Quick Links*:
                📓 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]
                
                ~{color:#888}-- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
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
                    "TLauncher",
                    "Titan"
                )
            )
        }

        object RemoveTriagedMeqs : ModuleConfigSpec() {
            val meqsTags by optional(listOf("MEQS_WAI", "MEQS_WONTFIX"))
            val removalReason by optional("Ticket has been triaged.")
        }

        object FutureVersion : ModuleConfigSpec() {
            val futureVersionMessage by optional(
                """
                {panel:borderColor=orange}(!) Please do not mark _Unreleased Versions_ as affected. You don't have access to them yet.{panel}
                
                ~{color:#888}- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
            )
        }

        object CHK : ModuleConfigSpec()

        object ReopenAwaiting : ModuleConfigSpec()

        object RemoveNonStaffMeqs : ModuleConfigSpec() {
            val removalReason by optional("Comment was not properly staff-restricted.")
        }

        object Empty : ModuleConfigSpec() {
            val emptyMessage by Crash.optional(
                """
                *Thank you for your report!*
                However, this issue is {color:#FF5722}*Incomplete*{color}.
                
                Your report does not contain enough information. As such, we're unable to understand or reproduce the problem.
                Please review the guidelines linked below before making further reports.
                
                In case of a game crash, be sure to attach the crashlog from {{[minecraft/crash-reports/crash-<DATE>-client.txt|https://minecrafthopper.net/help/guides/finding-minecraft-data-folder/]}}.
                
                *Quick Links*:
                📓 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]
                
                ~{color:#888}-- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
            )
        }

        object Crash : ModuleConfigSpec() {
            val maxAttachmentAge by optional(30)
            val crashExtensions by optional(listOf("txt", "log"))
            val duplicateMessage by optional(
                """
                *Thank you for your report!*
                We're actually already tracking this issue in *{DUPLICATE}*, so I've resolved and linked this ticket as a duplicate.
                
                Please take a look at the linked ticket and see if there is any fix available.
                
                If you need additional help with a technical problem, please visit [Community Support|https://discord.gg/58Sxm23].
                
                *Quick Links*:
                📓 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]
                
                ~{color:#888}-- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
            )
            val moddedMessage by optional(
                """
                *Thank you for your report!*
                However, this issue is {color:#FF5722}*Invalid*{color}.
                
                Your game, launcher or server is modified.
                If you can reproduce the issue in a vanilla environment, please recreate the issue.
                
                * Any non-standard client/server/launcher build needs to be taken up with the appropriate team, not Mojang.
                * Any plugin issues need to be addressed to the creator of the plugin or resource pack.
                * If you have problems on large servers, such as The Hive and Hypixel, please contact them first as they run modified server software.
                
                *Quick Links*:
                📓 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]
                
                ~{color:#888}-- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
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

        object RevokeConfirmation : ModuleConfigSpec()

        object KeepPrivate : ModuleConfigSpec() {
            val tag by optional("MEQS_KEEP_PRIVATE")
            val keepPrivateMessage by optional(
                """
                {panel:borderColor=orange}(!) Please do not mark issues as _private_, unless your bug report is an exploit or contains information about your username or server.{panel}

                ~{color:#888}-- I am a bot. This action was performed automagically! Please report any issues in [Discord|https://discordapp.com/invite/rpCyfKV] or [Reddit|https://www.reddit.com/r/Mojira/]{color}~
                """.trimIndent()
            )
        }

        object HideImpostors : ModuleConfigSpec()
    }
}

data class CrashDupeConfig(
    val type: String,
    val exceptionDesc: String,
    val duplicates: String
)
