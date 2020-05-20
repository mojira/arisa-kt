package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkParam
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class TransferLinksModuleTest : StringSpec({
    val DUPLICATES_LINK = getLink(
        issue = getLinkedIssue(
            key = "MC-1"
        )
    )

    val RELATES_LINK = getLink(
        type = "Relates",
        issue = getLinkedIssue(
            key = "MC-2"
        )
    )

    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferLinksModule()
        val issue = getIssue(("MC-1", emptyList(), emptyList())

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferLinksModule()
        val issue = getIssue("", listOf(RELATES_LINK), listOf(RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferLinksModule()
        val link = getLink(
            outwards = false,
            issue = getLinkedIssue(
                key = "MC-1"
            )
        )
        val issue = getIssue("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the issue has no additional links" {
        val module = TransferLinksModule()
        val issue = getIssue("", listOf(DUPLICATES_LINK), listOf(DUPLICATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing links to parent" {
        val module = TransferLinksModule()
        val issue = getIssue("", listOf(DUPLICATES_LINK, RELATES_LINK), listOf(DUPLICATES_LINK, RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove links" {
        var linkRemoved = false
        val module = TransferLinksModule()

        val linkToTransfer = getLink(
            type = "Relates",
            issue = getLinkedIssue(
                key = "MC-2"
            ),
            remove = { linkRemoved = true; Unit.right() }
        )

        val issue = getIssue("", listOf(DUPLICATES_LINK, linkToTransfer), listOf(DUPLICATES_LINK, linkToTransfer))

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        linkRemoved.shouldBeTrue()
    }

    "should not remove links towards the parent issue" {
        var parentLinkRemoved = false
        val module = TransferLinksModule()

        val duplicatesLink = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                getField = { listOf(RELATES_LINK).right() }
            ),
            remove = { parentLinkRemoved = true; Unit.right() }
        )
        val issue = getIssue("", listOf(duplicatesLink, RELATES_LINK), listOf(duplicatesLink, RELATES_LINK))

        val result = module(issue, NOW)

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
            true,
            getLinkedIssue(
                key = "MC-3"
            )
        ) { Unit.right() }

        val link = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                setField = { l ->
                    when (l.issue) {
                        "MC-1" -> parentLinkAdded = true
                        "MC-2" -> firstLinkAdded = true
                        "MC-3" -> secondLinkAdded = true
                    }
                    Unit.right()
                }
            )
        )
        val issue = getIssue(
            "", listOf(link, RELATES_LINK, relatesLink2), listOf(
                link,
                RELATES_LINK, relatesLink2
            )
        )
        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
        parentLinkAdded.shouldBeFalse()
    }

    "should add inwards links to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val duplicatesLink1 = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                setField = {
                    addedToFirstParent = true
                    Unit.right()
                }
            )
        )

        val duplicatesLink2 = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                setField = {
                    addedToSecondParent = true
                    Unit.right()
                }
            )
        )

        val issue = getIssue(
            "", listOf(duplicatesLink1, duplicatesLink2, RELATES_LINK), listOf(
                duplicatesLink1, duplicatesLink2,
                RELATES_LINK
            )
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should add parents to all outwards links" {
        var firstLinkAdded = false
        var secondLinkAdded = false
        val module = TransferLinksModule()

        val outwardsRelates1 = getLink(
            type = "Relates",
            outwards = false,
            issue = getLinkedIssue(
                key = "MC-2",
                setField = {
                    firstLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                }
            )
        )

        val outwardsRelates2 = getLink(
            type = "Relates",
            outwards = false,
            issue = getLinkedIssue(
                key = "MC-3",
                setField = {
                    secondLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                }
            )
        )

        val issue = getIssue(
            "",
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2),
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2)
        )
        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
    }

    "should add all parents to outwards links" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val outwardsRelates = getLink(
            type = "Relates",
            outwards = false,
            issue = getLinkedIssue(
                key = "MC-2",
                setField = { l ->
                    when (l.issue) {
                        "MC-1" -> addedToFirstParent = true
                        "MC-2" -> addedToSecondParent = true
                    }
                    Unit.right()
                }
            )
        )

        val duplicatesLink2 = getLink(
            issue = getLinkedIssue(
                key = "MC-2"
            )
        )

        val issue = getIssue(
            "",
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates),
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates)
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when removing a link fails" {
        val module = TransferLinksModule()

        val link = getLink(
            type = "Relates",
            issue = getLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val issue = getIssue("", listOf(DUPLICATES_LINK, link), listOf(DUPLICATES_LINK, link))

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when removing multiple links fails" {
        val module = TransferLinksModule()

        val link1 = getLink(
            type = "Relates",
            issue = getLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val link2 = getLink(
            type = "Relates",
            issue = getLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val issue = getIssue("", listOf(DUPLICATES_LINK, link1, link2), listOf(DUPLICATES_LINK, link1, link2))

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding a link fails" {
        val module = TransferLinksModule()

        val link = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                setField = { RuntimeException().left() }
            )
        )
        val issue = getIssue("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when adding multiple links fails" {
        val module = TransferLinksModule()

        val link = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                setField = { RuntimeException().left() }
            )
        )

        val issue = getIssue(
            "MC-1",
            listOf(link, RELATES_LINK, RELATES_LINK),
            listOf(link, RELATES_LINK, RELATES_LINK)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferLinksModule()
        val link = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                getField = { RuntimeException().left() }
            )
        )

        val issue = getIssue("", listOf(link, RELATES_LINK), listOf(link, RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferLinksModule()

        val link = getLink(
            issue = getLinkedIssue(
                key = "MC-1",
                getField = { RuntimeException().left() }
            )
        )

        val issue = getIssue("", listOf(link, link, RELATES_LINK), listOf(link, link, RELATES_LINK))

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})

private fun getLinkedIssue(
    key: String,
    status: String = "",
    setField: (field: LinkParam) -> Either<Throwable, Unit> = { Unit.right() },
    getField: () -> Either<Throwable, List<Link<*, LinkParam>>> = { emptyList<Link<*, LinkParam>>().right() }
) = LinkedIssue(
    key,
    status,
    setField,
    getField
)

private fun getLink(
    type: String = "Duplicate",
    outwards: Boolean = true,
    issue: LinkedIssue<List<Link<*, LinkParam>>, LinkParam>,
    remove: () -> Either<Throwable, Unit> = { Unit.right() }
): Link<List<Link<*, LinkParam>>, LinkParam> = Link(
    type,
    outwards,
    issue,
    remove
)
