package io.github.mojira.arisa.infrastructure

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import java.io.File

class IntegrationTest : StringSpec({
    "should be able to read the main config file correctly" {
        val config = Config { addSpec(Arisa) }
            // Only enable strict config parsing for map and YAML files; environment variables and system properties
            // likely contain entries completely unrelated to Arisa
            .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).map.flat(
                mapOf(
                    "arisa.credentials.accountId" to "test",
                    "arisa.credentials.username" to "test",
                    "arisa.credentials.password" to "test",
                    "arisa.credentials.email" to "test",
                    "arisa.credentials.apiToken" to "test",
                    "arisa.credentials.dandelionToken" to "test"
                )
            )
            .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).yaml.watchFile("config/config.yml")
            .from.env()
            .from.systemProperties()

        config.validateRequired()
    }

    "should contain required messages in helper-messages" {
        val helperMessagesFile = File("helper-messages.json")
        HelperMessageService.updateHelperMessages(helperMessagesFile)

        with(HelperMessageService.data.messages) {
            this shouldContainKey "attach-new-attachment"
            this shouldContainKey "duplicate"
            this shouldContainKey "duplicate-fixed"
            this shouldContainKey "duplicate-of-mc-297"
            this shouldContainKey "duplicate-of-mc-128302"
            this shouldContainKey "duplicate-of-mcl-5638"
            this shouldContainKey "duplicate-private"
            this shouldContainKey "duplicate-wai"
            this shouldContainKey "duplicate-wf"
            this shouldContainKey "pirated-minecraft"
            this shouldContainKey "provide-affected-versions"
            this shouldContainKey "incomplete"
            this shouldContainKey "modified-game"
            this shouldContainKey "not-reopen-ar"
            this shouldContainKey "panel-unmark-private-issue"
        }
    }
})
