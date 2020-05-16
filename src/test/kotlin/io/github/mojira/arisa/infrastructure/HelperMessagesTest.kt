package io.github.mojira.arisa.infrastructure

import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class HelperMessagesTest : StringSpec({
    val messages = HelperMessages.deserialize("""
        {
            "variables": {
                "quick_links": [
                    {
                        "project": "mc",
                        "value": "*Quick Links*:\n📓 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]",
                        "localedValue": {
                            "ab": "XYZ"
                        }
                    },
                    {
                        "project": "mcd",
                        "value": "*Quick Links*:\n📓 [Issue Guidelines|https://bugs.mojang.com/projects/MCD/summary] -- 💬 [Community Support|https://discord.gg/58Sxm23] -- 📧 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- 📖 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki] -- 📖 [FAQs|https://help.minecraft.net/hc/en-us/articles/360041345271]"
                    }
                ],
                "project_id": [
                    {
                        "project": "mc",
                        "value": "MC"
                    },
                    {
                        "project": "mcd",
                        "value": "MCD"
                    }
                ]
            },
            "messages": {
                "account-issue": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "Account issue",
                        "message": "*Thank you for your report!*\nHowever, this issue is {color:#FF5722}*Invalid*{color}.\n\nThis is an account issue. We do not have the tools to help you on this tracker.\nPlease contact the *[Customer Support|https://help.minecraft.net/hc/en-us/requests/new]*.\n\n%quick_links%",
                        "localedMessage": {
                            "ab": "ABC %quick_links%"
                        },
                        "fillname": []
                    }
                ],
                "duplicate-fixed": [
                    {
                        "project": ["mc", "mcd", "mcpe", "mcl", "mce"],
                        "name": "Duplicate (fixed)",
                        "message": "*Thank you for your report!*\nWe're actually already tracking this issue in *%s%*, so I've resolved and linked this ticket as a duplicate.\n\nThat ticket has already been resolved as Fixed. The fix will arrive in the next version or is already included in the latest development version of the game, you can check the Fix Version/s field in that ticket to learn more.\n\nIf you haven't already, you might like to make use of the [*+search feature+*|https://bugs.mojang.com/issues/?jql=project=%project_id%] to see if the issue has already been mentioned.\n\n%quick_links%",
                        "fillname": ["Enter parent issue ID"]
                    }
                ],
                "panel-private-issue": [
                    {
                        "project": "mc",
                        "name": "Panel - Private issue",
                        "message": "{panel:borderColor=orange}(!) Please do not mark issues as _private_, unless your bug report is an exploit or contains information about your username or server.{panel}\n",
                        "fillname": []
                    }
                ],
                "panel-future-version": [
                    {
                        "project": ["mc", "mcpe", "mcl", "mce"],
                        "name": "Panel - Future Version",
                        "message": "{panel:borderColor=orange}(!) Please do not mark _Unreleased Versions_ as affected. You don't have access to them yet.{panel}\n",
                        "fillname": []
                    }
                ]
            }
        }
    """.trimIndent())!!
    "should return Error when there is no such message" {
        val result = messages.getMessage("!@#%^&*", "MC")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key !@#%^&* under project MC"
    }
    "should return Error when the message doesn't have a value for the project" {
        val result = messages.getMessage("panel-private-issue", "MCTEST")

        result.shouldBeLeft()
        result.a should { it is Error }
        (result.a as Error).message shouldBe "Failed to find message for key panel-private-issue under project MCTEST"
    }
    "should return the message when the project matches a string filter" {
        val result = messages.getMessage("panel-private-issue", "MC")

        result.shouldBeRight()
        result.b shouldBe "{panel:borderColor=orange}(!) Please do not mark issues as _private_, unless your bug report is an exploit or contains information about your username or server.{panel}\n"
    }
    "should return the message when the project matches a list filter" {
        val result = messages.getMessage("panel-future-version", "MC")

        result.shouldBeRight()
        result.b shouldBe "{panel:borderColor=orange}(!) Please do not mark _Unreleased Versions_ as affected. You don't have access to them yet.{panel}\n"
    }
    "should replace the variable with the value for MC project" {
        val result = messages.getMessage("account-issue", "MC")

        result.shouldBeRight()
        result.b shouldBe "*Thank you for your report!*\nHowever, this issue is {color:#FF5722}*Invalid*{color}.\n\nThis is an account issue. We do not have the tools to help you on this tracker.\nPlease contact the *[Customer Support|https://help.minecraft.net/hc/en-us/requests/new]*.\n\n*Quick Links*:\n\uD83D\uDCD3 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- \uD83D\uDCAC [Community Support|https://discord.gg/58Sxm23] -- \uD83D\uDCE7 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- \uD83D\uDCD6 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]"
    }
    "should replace the variable with the value for MCD project" {
        val result = messages.getMessage("account-issue", "MCD")

        result.shouldBeRight()
        result.b shouldBe "*Thank you for your report!*\nHowever, this issue is {color:#FF5722}*Invalid*{color}.\n\nThis is an account issue. We do not have the tools to help you on this tracker.\nPlease contact the *[Customer Support|https://help.minecraft.net/hc/en-us/requests/new]*.\n\n*Quick Links*:\n\uD83D\uDCD3 [Issue Guidelines|https://bugs.mojang.com/projects/MCD/summary] -- \uD83D\uDCAC [Community Support|https://discord.gg/58Sxm23] -- \uD83D\uDCE7 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- \uD83D\uDCD6 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki] -- \uD83D\uDCD6 [FAQs|https://help.minecraft.net/hc/en-us/articles/360041345271]"
    }
    "should replace the variable with an empty string for MCPE project" {
        val result = messages.getMessage("account-issue", "MCPE")

        result.shouldBeRight()
        result.b shouldBe "*Thank you for your report!*\nHowever, this issue is {color:#FF5722}*Invalid*{color}.\n\nThis is an account issue. We do not have the tools to help you on this tracker.\nPlease contact the *[Customer Support|https://help.minecraft.net/hc/en-us/requests/new]*.\n\n"
    }
    "should replace the placeholder with filled text" {
        val result = messages.getMessage("duplicate-fixed", "MC", "MC-4")

        result.shouldBeRight()
        result.b shouldBe "*Thank you for your report!*\nWe're actually already tracking this issue in *MC-4*, so I've resolved and linked this ticket as a duplicate.\n\nThat ticket has already been resolved as Fixed. The fix will arrive in the next version or is already included in the latest development version of the game, you can check the Fix Version/s field in that ticket to learn more.\n\nIf you haven't already, you might like to make use of the [*+search feature+*|https://bugs.mojang.com/issues/?jql=project=MC] to see if the issue has already been mentioned.\n\n*Quick Links*:\n\uD83D\uDCD3 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- \uD83D\uDCAC [Community Support|https://discord.gg/58Sxm23] -- \uD83D\uDCE7 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- \uD83D\uDCD6 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]"
    }
    "should use the original value when the lang doesn't exist" {
        val result = messages.getMessage("account-issue", "MC", lang = "cd")

        result.shouldBeRight()
        result.b shouldBe "*Thank you for your report!*\nHowever, this issue is {color:#FF5722}*Invalid*{color}.\n\nThis is an account issue. We do not have the tools to help you on this tracker.\nPlease contact the *[Customer Support|https://help.minecraft.net/hc/en-us/requests/new]*.\n\n*Quick Links*:\n\uD83D\uDCD3 [Issue Guidelines|https://bugs.mojang.com/projects/MC/summary] -- \uD83D\uDCAC [Community Support|https://discord.gg/58Sxm23] -- \uD83D\uDCE7 [Customer Support|https://help.minecraft.net/hc/en-us/requests/new] -- ✍️ [Feedback and Suggestions|https://feedback.minecraft.net/] -- \uD83D\uDCD6 [Game Wiki|https://minecraft.gamepedia.com/Minecraft_Wiki]"
    }
    "should use the corresponding localed value" {
        val result = messages.getMessage("account-issue", "MC", lang = "ab")

        result.shouldBeRight()
        result.b shouldBe "ABC XYZ"
    }
})
