package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GreedyStringWithTrailingFlagArgumentTypeTest : StringSpec({
    val flag = "my-flag"
    val argumentType = greedyStringWithFlag(flag)

    "should work without flag" {
        val reader = StringReader("some text")
        val result = argumentType.parse(reader)
        result shouldBe StringWithFlag(
            "some text",
            false
        )
        reader.remainingLength shouldBe 0
    }

    "should work with flag" {
        val reader = StringReader("some text $flag")
        val result = argumentType.parse(reader)
        result shouldBe StringWithFlag(
            "some text",
            true
        )
        reader.remainingLength shouldBe 0
    }

    "should not detect as flag without space" {
        val reader = StringReader("some text$flag")
        val result = argumentType.parse(reader)
        result shouldBe StringWithFlag(
            "some text$flag",
            false
        )
        reader.remainingLength shouldBe 0
    }

    "should work with empty text and flag" {
        val reader = StringReader(" $flag")
        val result = argumentType.parse(reader)
        result shouldBe StringWithFlag(
            "",
            true
        )
        reader.remainingLength shouldBe 0
    }

    "should fail when only flag is used" {
        val reader = StringReader(flag)
        val exception = shouldThrow<CommandSyntaxException> {
            argumentType.parse(reader)
        }
        exception.message shouldBe "Argument consists only of flag 'my-flag' but does not contain a string at position 0: <--[HERE]"

        // Cursor should not have been changed
        reader.cursor shouldBe 0
    }
})
