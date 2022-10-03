package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AddLinksCommandTest : StringSpec({
    "should create links" {
        val command = AddLinksCommand()
        val list = mutableListOf<List<String>>()
        val issue = mockIssue(
            createLink = { type, key, outwards ->
                list.add(
                    listOf(
                        type, key,
                        outwards.toString()
                    )
                )
            }
        )

        command(issue, LinkList("relates to", listOf("MC-100", "MC-200"))) shouldBe 2
        list shouldBe (mutableListOf(listOf("Relates", "MC-100", "true"), listOf("Relates", "MC-200", "true")))
        list.clear()

        command(issue, LinkList("relates to", listOf("MC-100"))) shouldBe 1
        list shouldBe (mutableListOf(listOf("Relates", "MC-100", "true")))
        list.clear()

        command(issue, LinkList("is duplicated by", listOf("MC-100", "MC-200"))) shouldBe 2
        list shouldBe (mutableListOf(listOf("Duplicate", "MC-100", "false"), listOf("Duplicate", "MC-200", "false")))
        list.clear()

        command(issue, LinkList("is duplicated by", listOf("MC-100"))) shouldBe 1
        list shouldBe (mutableListOf(listOf("Duplicate", "MC-100", "false")))
    }
})
