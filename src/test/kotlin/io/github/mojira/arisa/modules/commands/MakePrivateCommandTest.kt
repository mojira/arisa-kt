package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MakePrivateCommandTest : StringSpec({
    val command = MakePrivateCommand()

    "should throw ALREADY_PRIVATE when the ticket is already set to private" {
        val issue = mockIssue(
                securityLevel = "private"
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue)
        }

        exception.message shouldBe "The ticket already had a security level set"
    }

    "should set ticket to private if the security level is null" {
        var hasSetPrivate = false

        val issue = mockIssue(
                securityLevel = null,
                setPrivate = { hasSetPrivate = true }
        )

        val result = command(issue)

        result shouldBe 1
        hasSetPrivate shouldBe true
    }
})
