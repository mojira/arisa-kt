package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class RemoveIdenticalLinkModuleTest : StringSpec({
    val module = RemoveIdenticalLinkModule()

    "should return OperationNotNeededModuleResponse when the ticket has no links" {
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the ticket has only one link" {
        val link = mockLink()
        val issue = mockIssue(
            links = listOf(link)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the links have directions" {
        val link1 = mockLink(
            outwards = true
        )
        val link2 = mockLink(
            outwards = false
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the links have different types" {
        val link1 = mockLink(
            type = "Duplicate"
        )
        val link2 = mockLink(
            type = "Relates"
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the links have different keys" {
        val link1 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1"
            )
        )
        val link2 = mockLink(
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

    "should remove extra link that is identical to the former one" {
        var hasRemovedLink1 = false
        var hasRemovedLink2 = false

        val link1 = mockLink(
            remove = { hasRemovedLink1 = true; Unit.right() }
        )
        val link2 = mockLink(
            remove = { hasRemovedLink2 = true; Unit.right() }
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        hasRemovedLink1 shouldBe false
        hasRemovedLink2 shouldBe true
    }

    "should remove all extra links that are identical to former ones" {
        var hasRemovedLink1 = false
        var hasRemovedLink2 = false
        var hasRemovedLink3 = false

        val link1 = mockLink(
            remove = { hasRemovedLink1 = true; Unit.right() }
        )
        val link2 = mockLink(
            remove = { hasRemovedLink2 = true; Unit.right() }
        )
        val link3 = mockLink(
            remove = { hasRemovedLink3 = true; Unit.right() }
        )
        val issue = mockIssue(
            links = listOf(link1, link2, link3)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        hasRemovedLink1 shouldBe false
        hasRemovedLink2 shouldBe true
        hasRemovedLink3 shouldBe true
    }

    "should return FailedModuleResponse when removing a link fails" {
        val link1 = mockLink()
        val link2 = mockLink(
            remove = { RuntimeException().left() }
        )
        val issue = mockIssue(
            links = listOf(link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions" {
        val link1 = mockLink()
        val link2 = mockLink(
            remove = { RuntimeException().left() }
        )
        val link3 = mockLink(
            remove = { RuntimeException().left() }
        )
        val issue = mockIssue(
            links = listOf(link1, link2, link3)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})
