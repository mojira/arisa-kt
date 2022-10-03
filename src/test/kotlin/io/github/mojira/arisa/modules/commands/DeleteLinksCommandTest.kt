package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DeleteLinksCommandTest : StringSpec({
    "should throw OperationNotNeededModuleResponse when given link type that can't be found in the issue" {
        val command = DeleteLinksCommand()

        shouldThrow<CommandSyntaxException> {
            command(
                mockIssue(
                    links = listOf(
                        mockLink(
                            type = "Duplicate"
                        )
                    )
                ),
                LinkList("relates to", listOf("MC-100"))
            )
        }

        shouldThrow<CommandSyntaxException> {
            command(
                mockIssue(
                    links = emptyList()
                ),
                LinkList("relates to", listOf("MC-100"))
            )
        }
    }

    "should delete links" {
        val command = DeleteLinksCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
            links = listOf(
                mockLink(
                    type = "Duplicate",
                    outwards = false,
                    remove = { list.add("duplicated1") },
                    issue = mockLinkedIssue(
                        key = "MC-100"
                    )
                ),
                mockLink(
                    type = "Duplicate",
                    outwards = false,
                    remove = { list.add("duplicated2") },
                    issue = mockLinkedIssue(
                        key = "MC-200"
                    )
                ),
                mockLink(
                    type = "Relates",
                    outwards = true,
                    remove = { list.add("relates1") },
                    issue = mockLinkedIssue(
                        key = "MC-100"
                    )
                ),
                mockLink(
                    type = "Relates",
                    outwards = false,
                    remove = { list.add("relates2") },
                    issue = mockLinkedIssue(
                        key = "MC-200"
                    )
                )
            )
        )

        command(issue, LinkList("is duplicated by", listOf("MC-100", "MC-200"))) shouldBe 2
        list shouldBe (mutableListOf("duplicated1", "duplicated2"))
        list.clear()

        command(issue, LinkList("is duplicated by", listOf("MC-100"))) shouldBe 1
        list shouldBe (mutableListOf("duplicated1"))
        list.clear()

        command(issue, LinkList("relates to", listOf("MC-100", "MC-200"))) shouldBe 2
        list shouldBe (mutableListOf("relates1", "relates2"))
        list.clear()

        command(issue, LinkList("relates to", listOf("MC-100"))) shouldBe 1
        list shouldBe (mutableListOf("relates1"))
    }
})
