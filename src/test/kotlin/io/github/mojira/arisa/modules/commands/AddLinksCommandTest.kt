package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.modules.addLinks
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AddLinksCommandTest : StringSpec ({
    "should return OperationNotNeededModuleResponse in Either when given wrong number of arguments is passed" {
        val command = AddLinksCommand()

        var result = command(mockIssue())
        result shouldBeLeft OperationNotNeededModuleResponse

        result = command(mockIssue(), "ARISA_ADD_LINKS", "relates")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when can't find type" {
        val command = AddLinksCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "MC-1", "MC-2")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when can't find ticket within first 4 arguments" {
        val command = AddLinksCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "is", "duplicated", "by", "something", "MC-2")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given not a ticket within the part with tickets" {
        val command = AddLinksCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "relates", "MC-1", "https://bugs.mojang.com/browse/MC-2", "lalala", "MC-3")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given type that does not exist" {
        val command = AddLinksCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "wrong", "MC-100")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given inconclusive type (that can match multiple link types)" {
        val command = AddLinksCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "is", "MC-100")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should create links" {
        val command = AddLinksCommand()
        val list = mutableListOf<List<String>>()
        val issue = mockIssue(
            createLink = { key, type, outwards -> list.add(listOf(key, type,
                    outwards.toString())) }
        )

        command(issue, "ARISA_ADD_LINKS", "relates", "MC-100", "MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Relates", "MC-100", "true"), listOf("Relates", "MC-200", "true")))
        list.clear()

        command(issue, "ARISA_ADD_LINKS", "relates", "MC-100") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Relates", "MC-100", "true")))
        list.clear()

        command(issue, "ARISA_ADD_LINKS", "duplicated", "MC-100", "MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Duplicate", "MC-100", "false"), listOf("Duplicate", "MC-200", "false")))
        list.clear()

        command(issue, "ARISA_ADD_LINKS", "duplicated", "MC-100") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Duplicate", "MC-100", "false")))
    }

    "should support urls" {
        val command = AddLinksCommand()
        val list = mutableListOf<List<String>>()
        val issue = mockIssue(
            createLink = { key, type, outwards -> list.add(listOf(key, type,
                    outwards.toString())) }
        )

        command(issue, "ARISA_ADD_LINKS", "relates", "https://bugs.mojang.com/browse/MC-100",
                "https://bugs.mojang.com/browse/MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Relates", "MC-100", "true"), listOf("Relates", "MC-200", "true")))
        list.clear()

        command(issue, "ARISA_ADD_LINKS", "duplicated", "https://bugs.mojang.com/browse/MC-100",
                "https://bugs.mojang.com/browse/MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Duplicate", "MC-100", "false"), listOf("Duplicate", "MC-200", "false")))
    }

    "should support types with spaces" {
        val command = AddLinksCommand()
        val list = mutableListOf<List<String>>()
        val issue = mockIssue(
            createLink = { key, type, outwards -> list.add(listOf(key, type,
                    outwards.toString())) }
        )

        command(issue, "ARISA_ADD_LINKS", "relates", "to", "MC-100", "MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Relates", "MC-100", "true"), listOf("Relates", "MC-200", "true")))
        list.clear()

        command(issue, "ARISA_ADD_LINKS", "is", "duplicated", "by", "MC-100", "MC-200")
                .shouldBeRight(ModuleResponse)
        list shouldBe(mutableListOf(listOf("Duplicate", "MC-100", "false"), listOf("Duplicate", "MC-200", "false")))
    }

    "type should be case insensitive" {
        val command = AddLinksCommand()
        val list = mutableListOf<List<String>>()
        val issue = mockIssue(
                createLink = { key, type, outwards -> list.add(listOf(key, type,
                        outwards.toString())) }
        )

        command(issue, "ARISA_ADD_LINKS", "relAtes To", "MC-100") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf(listOf("Relates", "MC-100", "true")))
    }
})