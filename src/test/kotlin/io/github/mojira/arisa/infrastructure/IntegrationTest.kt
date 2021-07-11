package io.github.mojira.arisa.infrastructure

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.ConfigService
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.config.MessageKeyItem
import io.github.mojira.arisa.infrastructure.config.MessageKeyMapItem
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainKeys

class IntegrationTest : StringSpec({
    "should be able to read the main config file correctly" {
        val config = Config { addSpec(Arisa) }
            .from.map.flat(
                mapOf(
                    "arisa.credentials.username" to "test",
                    "arisa.credentials.password" to "test",
                    "arisa.credentials.dandelionToken" to "test"
                )
            )
            .from.yaml.watchFile("config/config.yml")
            .from.env()
            .from.systemProperties()

        config.validateRequired()
    }

    "should contain required messages in helper-messages" {
        val helperMessagesFile = tempdir("helper-messages").resolve("helper-messages.json")
        val helperMessageService = HelperMessageService()
        helperMessageService.updateHelperMessages(helperMessagesFile)
        // Manually delete file because Kotest `tempdir` won't delete the directory otherwise,
        // see also https://github.com/kotest/kotest/pull/2227
        helperMessagesFile.delete()

        val messages = helperMessageService.data.messages
        messages.shouldContainKeys(
            "i-am-a-bot",
            "i-am-a-bot-dupe"
        )

        // Verify that messages exist for all keys used by config
        val config = ConfigService().config
        val messageKeysUsedByConfig = config.items.flatMap {
            when (it) {
                is MessageKeyItem -> listOf(config[it])
                is MessageKeyMapItem<*> -> config[it].values
                else -> emptyList()
            }
        }
        // Sanity check to verify that message key items are correctly identified
        messageKeysUsedByConfig.shouldNotBeEmpty()
        messageKeysUsedByConfig.forEach {
            messages shouldContainKey it
        }
    }
})
