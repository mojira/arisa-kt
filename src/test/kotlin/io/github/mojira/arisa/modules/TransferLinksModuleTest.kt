package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Link
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.LinkedIssue
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Request
import io.github.mojira.arisa.modules.TransferLinksModule.LinkParam
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class TransferLinksModuleTest : StringSpec({
    val DUPLICATES_LINK = Link(
        "Duplicate",
        true,
        LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
            "MC-1",
            "",
            { Unit.right() },
            { emptyList<Link<*, LinkParam>>().right() })
    ) { Unit.right() }

    val RELATES_LINK = Link(
        "Relates",
        false,
        LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
            "MC-2",
            "",
            { Unit.right() },
            { emptyList<Link<*, LinkParam>>().right() })
    ) { Unit.right() }
    
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferLinksModule()
        val request = Request<List<Link<*, LinkParam>>, LinkParam>("MC-1", emptyList(), emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferLinksModule()
        val request = Request("", listOf(RELATES_LINK), listOf(RELATES_LINK))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferLinksModule()
        val link = Link(
            "Duplicate",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { Unit.right() }, { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }
        val request = Request("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the issue has no additional links" {
        val module = TransferLinksModule()
        val request = Request("", listOf(DUPLICATES_LINK), listOf(DUPLICATES_LINK))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing links to parent" {
        val module = TransferLinksModule()
        val request = Request("", listOf(DUPLICATES_LINK, RELATES_LINK), listOf(DUPLICATES_LINK, RELATES_LINK))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove links" {
        var linkRemoved = false
        val module = TransferLinksModule()

        val linkToTransfer = Link(
            "Relates",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-2", "", { Unit.right() }, { emptyList<Link<*, LinkParam>>().right() })
        ) { linkRemoved = true; Unit.right() }

        val request = Request("", listOf(DUPLICATES_LINK, linkToTransfer), listOf(DUPLICATES_LINK, linkToTransfer))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linkRemoved.shouldBeTrue()
    }

    "should not remove links towards the parent issue" {
        var parentLinkRemoved = false
        val module = TransferLinksModule()

        val duplicatesLink = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { Unit.right() }, { listOf(RELATES_LINK).right() })
        ) { parentLinkRemoved = true; Unit.right() }
        val request = Request("", listOf(duplicatesLink, RELATES_LINK), listOf(duplicatesLink, RELATES_LINK))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        parentLinkRemoved.shouldBeFalse()
    }
    
    "should add all inwards links to parent" {
        var firstLinkAdded = false
        var secondLinkAdded = false
        var parentLinkAdded = false
        val module = TransferLinksModule()

        val relatesLink2 = Link(
            "Relates",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-3", "", { Unit.right() }, { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }

        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-1",
                "",
                { l ->
                    when (l.issue) {
                        "MC-1" -> parentLinkAdded = true
                        "MC-2" -> firstLinkAdded = true
                        "MC-3" -> secondLinkAdded = true
                    }
                    Unit.right()
                },
                { emptyList<Link<*, LinkParam>>().right() }
            )
        ) { Unit.right() }
        val request = Request("", listOf(link, RELATES_LINK, relatesLink2), listOf(link,
            RELATES_LINK, relatesLink2))
        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
        parentLinkAdded.shouldBeFalse()
    }

    "should add inwards links to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val duplicatesLink1 = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-1",
                "",
                {
                    addedToFirstParent = true
                    Unit.right()
                },
                { emptyList<Link<*, LinkParam>>().right() }
            )
        ) { Unit.right() }

        val duplicatesLink2 = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-1",
                "",
                {
                    addedToSecondParent = true
                    Unit.right()
                },
                { emptyList<Link<*, LinkParam>>().right() }
            )
        ) { Unit.right() }

        val request = Request("", listOf(duplicatesLink1, duplicatesLink2, RELATES_LINK), listOf(duplicatesLink1, duplicatesLink2,
            RELATES_LINK
        ))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should add parents to all outwards links" {
        var firstLinkAdded = false
        var secondLinkAdded = false
        val module = TransferLinksModule()
        
        val outwardsRelates1 = Link(
            "Relates",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                {
                    firstLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }

        val outwardsRelates2 = Link(
            "Relates",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-3",
                "",
                {
                    secondLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }
        
        val request = Request(
            "",
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2),
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2)
        )
        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
    }

    "should add all parents to outwards links" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val outwardsRelates = Link(
            "Relates",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                { l ->
                    when (l.issue) {
                        "MC-1" -> addedToFirstParent = true
                        "MC-2" -> addedToSecondParent = true
                    }
                    Unit.right()
                },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }

        val duplicatesLink2 = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                { Unit.right() },
                { emptyList<Link<*, LinkParam>>().right() }
            )
        ) { Unit.right() }

        val request = Request(
            "",
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates),
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates)
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when removing a link fails" {
        val module = TransferLinksModule()

        val link = Link(
            "Relates",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                { Unit.right() },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { RuntimeException().left() }

        val request = Request("", listOf(DUPLICATES_LINK, link), listOf(DUPLICATES_LINK, link))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when removing multiple links fails" {
        val module = TransferLinksModule()

        val link1 = Link(
            "Relates",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                { Unit.right() },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { RuntimeException().left() }

        val link2 = Link(
            "Relates",
            false,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>(
                "MC-2",
                "",
                { Unit.right() },
                { emptyList<Link<*, LinkParam>>().right() })
        ) { RuntimeException().left() }

        val request = Request("", listOf(DUPLICATES_LINK, link1, link2), listOf(DUPLICATES_LINK, link1, link2))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding a link fails" {
        val module = TransferLinksModule()

        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { RuntimeException().left() }, { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }
        val request = Request("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when adding multiple links fails" {
        val module = TransferLinksModule()

        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { RuntimeException().left() }, { emptyList<Link<*, LinkParam>>().right() })
        ) { Unit.right() }

        val request = Request(
            "MC-1",
            listOf(link, RELATES_LINK, RELATES_LINK),
            listOf(link, RELATES_LINK, RELATES_LINK)
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferLinksModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { Unit.right() }, { RuntimeException().left() })
        ) { Unit.right() }

        val request = Request("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferLinksModule()

        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Link<*, LinkParam>>, LinkParam>("MC-1", "", { Unit.right() }, { RuntimeException().left() })
        ) { Unit.right() }

        val request = Request("", listOf(link, link, RELATES_LINK), listOf(link, link, RELATES_LINK))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})
