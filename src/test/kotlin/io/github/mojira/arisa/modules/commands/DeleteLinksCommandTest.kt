package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.modules.deleteLinks
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DeleteLinksCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse in Either when given wrong number of arguments is passed" {
        val command = DeleteLinksCommand()

        var result = command(mockIssue())
        result shouldBeLeft OperationNotNeededModuleResponse

        result = command(mockIssue(), "ARISA_REMOVE_LINKS", "relates")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when can't find type" {
        val command = DeleteLinksCommand()

        val result = command(mockIssue(), "ARISA_REMOVE_LINKS", "MC-1", "MC-2")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when can't find ticket within first 4 arguments" {
        val command = DeleteLinksCommand()

        val result = command(mockIssue(), "ARISA_REMOVE_LINKS", "is", "duplicated", "by", "something", "MC-2")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given not a ticket within the part with tickets" {
        val command = DeleteLinksCommand()

        val result = command(mockIssue(), "ARISA_REMOVE_LINKS", "relates", "MC-1", "https://bugs.mojang.com/browse/MC-2", "lalala", "MC-3")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given type that does not exist" {
        val command = DeleteLinksCommand()

        val result = command(mockIssue(), "ARISA_REMOVE_LINKS", "wrong", "MC-100")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given inconclusive type (that can match multiple link types)" {
        val command = DeleteLinksCommand()

        val result = command(mockIssue(), "ARISA_REMOVE_LINKS", "is", "MC-100")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should return OperationNotNeededModuleResponse in Either when given link type that can't be found in the issue" {
        val command = DeleteLinksCommand()

        command(mockIssue(
                links = listOf(mockLink(
                        type = "Duplicate"
                ))
        ), "ARISA_REMOVE_LINKS", "relates", "MC-100") shouldBeLeft OperationNotNeededModuleResponse

        command(mockIssue(
                links = emptyList()
        ), "ARISA_REMOVE_LINKS", "relates", "MC-100") shouldBeLeft OperationNotNeededModuleResponse
    }

    "should create links" {
        val command = DeleteLinksCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Duplicate",
                        outwards = false,
                        remove = { list.add("duplicated1") },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Duplicate",
                        outwards = false,
                        remove = { list.add("duplicated2") },
                        issue = mockLinkedIssue(
                                key = "MC-200"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("relates1") },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = false,
                        remove = { list.add("relates2") },
                        issue = mockLinkedIssue(
                                key = "MC-200"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "duplicated", "MC-100", "MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("duplicated1", "duplicated2"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "duplicated", "MC-100") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("duplicated1"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "relates", "MC-100", "MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("relates1", "relates2"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "relates", "MC-100") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("relates1"))
    }

    "should support urls" {
        val command = DeleteLinksCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Duplicate",
                        outwards = false,
                        remove = { list.add("duplicated1") },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Duplicate",
                        outwards = false,
                        remove = { list.add("duplicated2") },
                        issue = mockLinkedIssue(
                                key = "MC-200"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("relates1") },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = false,
                        remove = { list.add("relates2") },
                        issue = mockLinkedIssue(
                                key = "MC-200"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "duplicated", "https://bugs.mojang.com/browse/MC-100",
                "https://bugs.mojang.com/browse/MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("duplicated1", "duplicated2"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "duplicated", "https://bugs.mojang.com/browse/MC-100")
                .shouldBeRight(ModuleResponse)
        list shouldBe(mutableListOf("duplicated1"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "relates", "https://bugs.mojang.com/browse/MC-100",
                "https://bugs.mojang.com/browse/MC-200") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("relates1", "relates2"))
        list.clear()

        command(issue, "ARISA_REMOVE_LINKS", "relates", "https://bugs.mojang.com/browse/MC-100")
                .shouldBeRight(ModuleResponse)
        list shouldBe(mutableListOf("relates1"))
    }

    "should support types with spaces" {
        val command = DeleteLinksCommand()
        var linkVar = ""
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Duplicate",
                        outwards = false,
                        remove = { linkVar = "duplicated" },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { linkVar = "relates" },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "is", "duplicated", "by", "MC-100") shouldBeRight ModuleResponse
        linkVar shouldBe("duplicated")
        linkVar = ""

        command(issue, "ARISA_REMOVE_LINKS", "relates", "to", "MC-100") shouldBeRight ModuleResponse
        linkVar shouldBe("relates")
    }

    "type should be case insensitive" {
        val command = DeleteLinksCommand()
        var linkVar = ""
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { linkVar = "relates" },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "relAtes", "To", "MC-100") shouldBeRight ModuleResponse
        linkVar shouldBe("relates")
    }

    "should support commas" {
        val command = DeleteLinksCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("1") },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("2") },
                        issue = mockLinkedIssue(
                                key = "MC-200"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("3") },
                        issue = mockLinkedIssue(
                                key = "MC-300"
                        )
                ), mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { list.add("4") },
                        issue = mockLinkedIssue(
                                key = "MC-400"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "relates", "MC-100,", "MC-200,MC-300", ",MC-400") shouldBeRight ModuleResponse
        list shouldBe(mutableListOf("1", "2", "3", "4"))
    }

    "keys should be case insensitive" {
        val command = DeleteLinksCommand()
        var linkVar = ""
        val issue = mockIssue(
                links = listOf(mockLink(
                        type = "Relates",
                        outwards = true,
                        remove = { linkVar = "relates" },
                        issue = mockLinkedIssue(
                                key = "MC-100"
                        )
                ))
        )

        command(issue, "ARISA_REMOVE_LINKS", "relates", "mc-100") shouldBeRight ModuleResponse
        linkVar shouldBe("relates")
    }
})