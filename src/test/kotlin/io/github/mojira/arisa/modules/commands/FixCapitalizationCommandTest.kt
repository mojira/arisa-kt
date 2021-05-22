package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FixCapitalizationCommandTest : StringSpec({
    val command = FixCapitalizationCommand()

    "should throw NO_CAPITALIZATION_MATCHES when the description has no improper capitalization" {
        val issue = mockIssue(
            description = "testing without capitalization."
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue)
        }

        exception.message shouldBe "No incorrect capitalization matches were found"
    }

    "should replace capitalized sentences in description" {
        var hasUpdatedDescription: String? = null

        val issue = mockIssue(
            description = "Testing With Capitalization.",
            updateDescription = {
                hasUpdatedDescription = it
            }
        )

        val result = command(issue)

        result shouldBe 1
        hasUpdatedDescription shouldBe "Testing with capitalization."
    }

    "should capitalize exceptions" {
        var hasUpdatedDescription: String? = null

        val issue = mockIssue(
            description = "this is not properly capitalized and i will fix it",
            updateDescription = {
                hasUpdatedDescription = it
            }
        )

        val result = command(issue)

        result shouldBe 1
        hasUpdatedDescription shouldBe "this is not properly capitalized and I will fix it"
    }
})
