package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RemoveIdenticalLinkModuleTest : StringSpec({
    val module = RemoveIdenticalLinkModule()

    "should return OperationNotNeededModuleResponse when the ticket has no links" {
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the links are not Relates" {
        val link1 = mockLink(
            outwards = true
        )
        val link2 = mockLink(
            outwards = true
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the ticket has only one link" {
        val link = mockLink(
            type = "Relates"
        )
        val issue = mockIssue(
            links = listOf(link)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the links have different keys" {
        val link1 = mockLink(
            type = "Relates",
            issue = mockLinkedIssue(
                key = "MC-1"
            )
        )
        val link2 = mockLink(
            type = "Relates",
            issue = mockLinkedIssue(
                key = "MC-2"
            )
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove the extra Relates link pointing out from the host when the host is smaller" {
        var hasRemovedLink1 = false
        var hasRemovedLink2 = false

        val link1 = mockLink(
            type = "Relates",
            outwards = true,
            remove = { hasRemovedLink1 = true },
            issue = mockLinkedIssue(
                key = "MC-2"
            )
        )
        val link2 = mockLink(
            type = "Relates",
            outwards = false,
            remove = { hasRemovedLink2 = true },
            issue = mockLinkedIssue(
                key = "MC-2"
            )
        )

        val issue = mockIssue(
            key = "MC-1",
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        hasRemovedLink1 shouldBe false
        hasRemovedLink2 shouldBe true
    }

    "should remove the extra Relates link pointing out from the target when the target is smaller" {
        var hasRemovedLink1 = false
        var hasRemovedLink2 = false

        val link1 = mockLink(
            type = "Relates",
            outwards = true,
            remove = { hasRemovedLink1 = true },
            issue = mockLinkedIssue(
                key = "MC-1"
            )
        )
        val link2 = mockLink(
            type = "Relates",
            outwards = false,
            remove = { hasRemovedLink2 = true },
            issue = mockLinkedIssue(
                key = "MC-1"
            )
        )

        val issue = mockIssue(
            key = "MC-2",
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        hasRemovedLink1 shouldBe true
        hasRemovedLink2 shouldBe false
    }
})
