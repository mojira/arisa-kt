package io.github.mojira.arisa.infrastructure

import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class HelperMessagesTest : StringSpec({
    val messages = HelperMessages.deserialize(
        """
        {
            "variables": {
                "variable": [
                    {
                        "project": "mc",
                        "value": "variable for MC",
                        "localizedValues": {
                            "ab": "abababab aba MC"
                        }
                    },
                    {
                        "project": "mcd",
                        "value": "variable for MCD"
                    }
                ]
            },
            "messages": {
                "normal": [
                    {
                        "project": "mc",
                        "name": "Normal message",
                        "message": "Normal message",
                        "localizedMessages": {
                            "ab": "Ababab abababa"
                        },
                        "fillname": []
                    }
                ],
                "normal-list-filter": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "Normal message with a list filter",
                        "message": "Normal message with a list filter",
                        "fillname": []
                    }
                ],
                "with-variable": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "With variable",
                        "message": "With %variable%",
                        "localizedMessages": {
                            "ab": "Abab %variable%"
                        },
                        "fillname": []
                    }
                ],
                "with-placeholder": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "With placeholder",
                        "message": "With %s%",
                        "fillname": ["Placeholder"]
                    }
                ],
                "i-am-a-bot": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "I am a Bot",
                        "message": "~{color:#888}-- I am a bot.{color}~",
                        "localizedMessages": {
                            "ab": "~{color:#888}-- A ba b aba.{color}~"
                        },
                        "fillname": []
                    }
                ]
            }
        }
    """.trimIndent()
    )!!

    "should return Error when there is no such message" {
        val result = messages.getSingleMessage("MC", "!@#%^&*")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key !@#%^&* under project MC"
    }

    "should return Error when the message doesn't have a value for the project" {
        val result = messages.getSingleMessage("MCTEST", "normal")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key normal under project MCTEST"
    }

    "should return the message when the project matches a string filter" {
        val result = messages.getSingleMessage("MC", "normal")

        result.shouldBeRight()
        result.b shouldBe "Normal message"
    }

    "should return the message when the project matches a list filter" {
        val result = messages.getSingleMessage("MC", "normal-list-filter")

        result.shouldBeRight()
        result.b shouldBe "Normal message with a list filter"
    }

    "should replace the variable with the value for MC project" {
        val result = messages.getSingleMessage("MC", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With variable for MC"
    }

    "should replace the variable with the value for MCD project" {
        val result = messages.getSingleMessage("MCD", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With variable for MCD"
    }

    "should replace the variable with an empty string for MCPE project" {
        val result = messages.getSingleMessage("MCPE", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With "
    }

    "should replace the placeholder with filled text" {
        val result = messages.getSingleMessage("MC", "with-placeholder", "MC-4")

        result.shouldBeRight()
        result.b shouldBe "With MC-4"
    }

    "should use the original value when the lang doesn't exist" {
        val result = messages.getSingleMessage("MC", "normal", lang = "cd")

        result.shouldBeRight()
        result.b shouldBe "Normal message"
    }

    "should combine multiple messages correctly" {
        val result = messages.getMessage("MC", listOf("normal", "i-am-a-bot"))

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should prepend localized messages" {
        val result = messages.getMessage("MC", listOf("normal", "i-am-a-bot"), lang = "ab")

        result shouldBe "Ababab abababa\n~{color:#888}-- A ba b aba.{color}~" +
                "\n----\n" +
                "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should only contain original message when the lang doesn't exist" {
        val result = messages.getMessage("MC", listOf("normal", "i-am-a-bot"), lang = "cd")

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should append the bot signature correctly" {
        val result = messages.getMessageWithBotSignature("MC", "normal")

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }
})
