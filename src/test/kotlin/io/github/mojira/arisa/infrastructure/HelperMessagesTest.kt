package io.github.mojira.arisa.infrastructure

import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class HelperMessagesTest : StringSpec({
    HelperMessageService.setHelperMessages(
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
                ],
                "varception0": [
                    {
                        "project": "mctest",
                        "value": "0"
                    }
                ],
                "varception1": [
                    {
                        "project": "mctest",
                        "value": "1 %varception0%"
                    }
                ],
                "varception2": [
                    {
                        "project": "mctest",
                        "value": "2 %varception1%"
                    }
                ],
                "varception3": [
                    {
                        "project": "mctest",
                        "value": "3 %varception2%"
                    }
                ],
                "infinite_loop": [
                    {
                        "project": "mctest",
                        "value": ".%infinite_loop%"
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
                        "project": ["mc", "mcd", "mcpe", "mcl"],
                        "name": "Normal message with a list filter",
                        "message": "Normal message with a list filter",
                        "fillname": []
                    }
                ],
                "with-variable": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl"],
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
                        "project": ["mc", "mcd", "mcpe", "mcl"],
                        "name": "With placeholder",
                        "message": "With %s%",
                        "fillname": ["Placeholder"]
                    }
                ],
                "i-am-a-bot": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl"],
                        "name": "I am a Bot",
                        "message": "~{color:#888}-- I am a bot.{color}~",
                        "localizedMessages": {
                            "ab": "~{color:#888}-- A ba b aba.{color}~"
                        },
                        "fillname": []
                    }
                ],
                "infinite-loop": [
                    {
                        "project": ["mctest"],
                        "name": "Infinite Loop",
                        "message": "%infinite_loop%",
                        "fillname": []
                    }
                ],
                "varception": [
                    {
                        "project": ["mctest"],
                        "name": "Varception",
                        "message": "%varception3%",
                        "fillname": []
                    }
                ]
            }
        }
    """.trimIndent()
    )

    "should return Error when there is no such message" {
        val result = HelperMessageService.getSingleMessage("MC", "!@#%^&*")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key !@#%^&* under project MC"
    }

    "should return Error when the message doesn't have a value for the project" {
        val result = HelperMessageService.getSingleMessage("MCTEST", "normal")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key normal under project MCTEST"
    }

    "should return the message when the project matches a string filter" {
        val result = HelperMessageService.getSingleMessage("MC", "normal")

        result.shouldBeRight()
        result.b shouldBe "Normal message"
    }

    "should return the message when the project matches a list filter" {
        val result = HelperMessageService.getSingleMessage("MC", "normal-list-filter")

        result.shouldBeRight()
        result.b shouldBe "Normal message with a list filter"
    }

    "should replace the variable with the value for MC project" {
        val result = HelperMessageService.getSingleMessage("MC", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With variable for MC"
    }

    "should replace the variable with the value for MCD project" {
        val result = HelperMessageService.getSingleMessage("MCD", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With variable for MCD"
    }

    "should replace the variable with an empty string for MCPE project" {
        val result = HelperMessageService.getSingleMessage("MCPE", "with-variable")

        result.shouldBeRight()
        result.b shouldBe "With "
    }

    "should be able to handle variables containing other variables" {
        val result = HelperMessageService.getSingleMessage("MCTEST", "varception")

        result.shouldBeRight()
        result.b shouldBe "3 2 1 0"
    }

    "should not get caught in an infinite loop when a variable contains itself" {
        val result = HelperMessageService.getSingleMessage("MCTEST", "infinite-loop")

        result.shouldBeRight()
        result.b shouldBe "..........%infinite_loop%"
    }

    "should replace the placeholder with filled text" {
        val result = HelperMessageService.getSingleMessage("MC", "with-placeholder", "MC-4")

        result.shouldBeRight()
        result.b shouldBe "With MC-4"
    }

    "should use the original value when the lang doesn't exist" {
        val result = HelperMessageService.getSingleMessage("MC", "normal", lang = "cd")

        result.shouldBeRight()
        result.b shouldBe "Normal message"
    }

    "should combine multiple messages correctly" {
        val result = HelperMessageService.getMessage("MC", listOf("normal", "i-am-a-bot"))

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should prepend localized messages" {
        val result = HelperMessageService.getMessage("MC", listOf("normal", "i-am-a-bot"), lang = "ab")

        result shouldBe "Ababab abababa\n~{color:#888}-- A ba b aba.{color}~" +
                "\n----\n" +
                "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should only contain original message when the lang doesn't exist" {
        val result = HelperMessageService.getMessage("MC", listOf("normal", "i-am-a-bot"), lang = "cd")

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }

    "should append the bot signature correctly" {
        val result = HelperMessageService.getMessageWithBotSignature("MC", "normal")

        result shouldBe "Normal message\n~{color:#888}-- I am a bot.{color}~"
    }
})
