package io.github.mojira.arisa.infrastructure

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContainKey
import java.io.File

class IntegrationTest : StringSpec({
    "should be able to read the JSON correctly" {
        val config = Config { addSpec(Arisa) }
            .from.map.flat(
                mapOf(
                    "arisa.credentials.username" to "test",
                    "arisa.credentials.password" to "test",
                    "arisa.credentials.dandelionToken" to "test"
                )
            )
            .from.json.watchFile("arisa.json")
            .from.env()
            .from.systemProperties()

        config.containsRequired().shouldBeTrue()
    }

    "should contain required messages in helper-messages" {
        val helperMessagesFile = File("helper-messages.json")
        val helperMessages = helperMessagesFile.getHelperMessages()

        with(helperMessages.messages) {
            this shouldContainKey "pirated-minecraft"
            this shouldContainKey "provide-affected-versions"
            this shouldContainKey "incomplete"
            this shouldContainKey "modified-game"
            this shouldContainKey "panel-unmark-private-issue"
        }
    }
})
