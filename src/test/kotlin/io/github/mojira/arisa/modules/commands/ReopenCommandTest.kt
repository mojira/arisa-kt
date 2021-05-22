package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReopenCommandTest : StringSpec({
    val command = ReopenCommand()

    "should throw NOT_AR when the resolution is not awaiting response" {
        val issue = mockIssue(
            resolution = "Invalid"
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue)
        }

        exception.message shouldBe "The ticket was not resolved as Awaiting Response"
    }

    "should reopen issue if it is resolved as awaiting response" {
        var reopened = false

        val issue = mockIssue(
            resolution = "Awaiting Response",
            reopen = {
                reopened = true
            }
        )

        val result = command(issue)

        result shouldBe 1
        reopened shouldBe true
    }
})
