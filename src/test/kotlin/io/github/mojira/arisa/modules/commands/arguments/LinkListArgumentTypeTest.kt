package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LinkListArgumentTypeTest : StringSpec({
    val argumentType = LinkListArgumentType()

    "should support IDs" {
        val reader = StringReader("relates MC-100 MC-200")
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relates",
            listOf("MC-100", "MC-200")
        )
        reader.remainingLength shouldBe 0
    }

    "should support URLs" {
        val reader = StringReader(
            "relates https://bugs.mojang.com/browse/MC-100 " +
                "https://bugs.mojang.com/browse/MC-200"
        )
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relates",
            listOf("MC-100", "MC-200")
        )
        reader.remainingLength shouldBe 0
    }

    "should support types with spaces" {
        val reader = StringReader("relates to MC-100 MC-200")
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relates to",
            listOf("MC-100", "MC-200")
        )
        reader.remainingLength shouldBe 0
    }

    "should support types case-insensitively" {
        val reader = StringReader("relAtes To MC-100 MC-200")
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relAtes To",
            listOf("MC-100", "MC-200")
        )
        reader.remainingLength shouldBe 0
    }

    "should support commas" {
        val reader = StringReader("relates MC-100, MC-200,MC-300 ,MC-400")
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relates",
            listOf("MC-100", "MC-200", "MC-300", "MC-400")
        )
        reader.remainingLength shouldBe 0
    }

    "should support ticket keys case-insensitively" {
        val reader = StringReader("relates mc-100")
        val result = argumentType.parse(reader)
        result shouldBe LinkList(
            "relates",
            listOf("mc-100")
        )
        reader.remainingLength shouldBe 0
    }

    "should throw exception when there isn't enough segments" {
        val reader = StringReader("relates")
        shouldThrow<CommandSyntaxException> {
            argumentType.parse(reader)
        }
    }

    "should throw exception when can't find type" {
        val reader = StringReader("MC-1 MC-2")
        shouldThrow<CommandSyntaxException> {
            argumentType.parse(reader)
        }
    }

    "should throw exception when can't find ticket ID" {
        val reader = StringReader("is duplicated by something MC-2")
        shouldThrow<CommandSyntaxException> {
            argumentType.parse(reader)
        }
    }

    "should throw exception when given invalid ticket IDs" {
        val reader = StringReader("relates MC-1 https://bugs.mojang.com/browse/MC-2 lalala MC-3")
        shouldThrow<CommandSyntaxException> {
            argumentType.parse(reader)
        }
    }
})
