package io.github.mojira.arisa.infrastructure.config

import com.uchuhimo.konf.ConfigSpec

object Arisa : ConfigSpec() {
    val shadow by optional(false)
    object Credentials : ConfigSpec() {
        val username by required<String>()
        val password by required<String>()
    }

    object Issues : ConfigSpec() {
        val projects by optional("MC,MCTEST,MCPE,MCAPI,MCL,MC,MCE")
        val url by optional("https://bugs.mojang.com/")
        val checkInterval by optional(10L)
    }

    object CustomFields : ConfigSpec() {
        val chkField by optional("customfield_10701")
        val confirmationField by optional("customfield_10500")
    }

    object Modules : ConfigSpec() {
        open class ModuleConfigSpec : ConfigSpec() {
            val whitelist by optional("MC,MCTEST,MCPE,MCAPI,MCL,MC")
        }

        object Attachment : ModuleConfigSpec() {
            val extensionBlacklist by optional("jar,exe,com,bat,msi,run,lnk,dmg")
        }

        object Piracy : ModuleConfigSpec() {
            val piracyMessage by optional(
                "You are currently using a *non-authorized* version of Minecraft." +
                    " If you wish to purchase the full game, please visit the [Minecraft Store|https://minecraft.net/store].\r\n" +
                    "We will not provide support for pirated versions of the game, these versions are modified and may contain malware."
            )
            val piracySignatures by optional("Minecraft Launcher null,Bootstrap 0,Launcher: 1.0.10  (bootstrap 4),Launcher: 1.0.10  (bootstrap 5),Launcher 3.0.0,Launcher: 3.1.0,Launcher: 3.1.1,Launcher: 3.1.4,1.0.8,uuid sessionId,auth_access_token,windows-\${arch},keicraft,keinett,nodus,iridium,mcdonalds,uranium,nova,divinity,gemini,mineshafter,Team-NeO,DarkLBP,Launcher X,PHVL,Pre-Launcher v6,LauncherFEnix,TLauncher")
        }

        object CHK : ModuleConfigSpec()

        object ReopenAwaiting : ModuleConfigSpec()
    }
}
