package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * MC-42 duplicates MC-1.
 */
private val DUPLICATE_LINK = linkIssues(
    key1 = "MC-42",
    key2 = "MC-1"
)

/**
 * MC-42 relates to MC-10.
 */
private val RELATES_LINK = linkIssues(
    key1 = "MC-42",
    key2 = "MC-10",
    type = "Relates"
)

class TransferLinksModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42"
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferLinksModule()
        val link = linkIssues(
            key1 = "MC-1",
            key2 = "MC-42",
            outwards = false
        )
        val issue = mockIssue(
            key = "MC-1",
            links = listOf(link)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the issue has no additional links" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATE_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing links to parent" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATE_LINK, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove links" {
        var linkRemoved = false
        val module = TransferLinksModule()

        /**
         * MC-42 relates to MC-10.
         */
        val linkToTransfer = linkIssues(
            key1 = "MC-42",
            key2 = "MC-10",
            type = "Relates",
            removeLink = { key1, type, key2 ->
                linkRemoved=true
                key1 shouldBe "MC-42"
                type shouldBe "Relates"
                key2 shouldBe "MC-10"
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATE_LINK, linkToTransfer)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        linkRemoved.shouldBeTrue()
    }

    "should not remove links towards the parent issue" {
        var parentLinkRemoved = false
        val module = TransferLinksModule()

        /**
         * MC-42 duplicates MC-1.
         */
        val duplicatesLink = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            removeLink = { _, _, _ ->
                parentLinkRemoved=true
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        parentLinkRemoved.shouldBeFalse()
    }

    "should add all inwards links to parent" {
        var firstLinkAdded = false
        var secondLinkAdded = false
        var parentLinkAdded = false
        val module = TransferLinksModule()

        /**
         * MC-100 duplicates MC-42.
         */
        val link1 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-100",
            outwards = false,
            createLink = { key1, type, key2 ->
                firstLinkAdded = true
                key1 shouldBe "MC-100"
                type shouldBe "Duplicate"
                key2 shouldBe "MC-1"
            }
        )

        /**
         * MC-101 duplicates MC-42.
         */
        val link2 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-101",
            outwards = false,
            createLink = { key1, type, key2 ->
                secondLinkAdded = true
                key1 shouldBe "MC-101"
                type shouldBe "Duplicate"
                key2 shouldBe "MC-1"
            }
        )

        /**
         * MC-42 duplicates MC-1.
         */
        val link = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = { _, _, _ ->
                parentLinkAdded = true
            }
        )

        val issue = mockIssue(
            links = listOf(link, link1, link2)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
        parentLinkAdded.shouldBeFalse()
    }

    "should add inwards links to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val duplicatesLink1 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                setField = {
                    addedToFirstParent = true
                    Unit.right()
                }
            )
        )

        val duplicatesLink2 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                setField = {
                    addedToSecondParent = true
                    Unit.right()
                }
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            listOf(duplicatesLink1, duplicatesLink2, RELATES_LINK), listOf(
                duplicatesLink1, duplicatesLink2,
                RELATES_LINK
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should add parents to all outwards links" {
        var firstLinkAdded = false
        var secondLinkAdded = false
        val module = TransferLinksModule()

        val outwardsRelates1 = mockLink(
            type = "Relates",
            outwards = false,
            issue = mockLinkedIssue(
                key = "MC-2",
                setField = {
                    firstLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                }
            )
        )

        val outwardsRelates2 = mockLink(
            type = "Relates",
            outwards = false,
            issue = mockLinkedIssue(
                key = "MC-3",
                setField = {
                    secondLinkAdded = true
                    it.type.shouldBe("Relates")
                    it.issue.shouldBe("MC-1").right()
                }
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2),
            listOf(DUPLICATES_LINK, outwardsRelates1, outwardsRelates2)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        firstLinkAdded.shouldBeTrue()
        secondLinkAdded.shouldBeTrue()
    }

    "should add all parents to outwards links" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferLinksModule()

        val outwardsRelates = mockLink(
            type = "Relates",
            outwards = false,
            issue = mockLinkedIssue(
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

        val duplicatesLink2 = mockLink(
            key = "MC-42",
            issue = mockLinkedIssue(
                key = "MC-2"
            )
        )

        val issue = mockIssue(
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates),
            listOf(DUPLICATES_LINK, duplicatesLink2, outwardsRelates)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when removing a link fails" {
        val module = TransferLinksModule()

        val link = mockLink(
            type = "Relates",
            issue = mockLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val issue = mockIssue(
            key = "MC-42",
            listOf(DUPLICATES_LINK, link),
            listOf(DUPLICATES_LINK, link)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when removing multiple links fails" {
        val module = TransferLinksModule()

        val link1 = mockLink(
            type = "Relates",
            issue = mockLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val link2 = mockLink(
            key = "MC-42",
            type = "Relates",
            issue = mockLinkedIssue(
                key = "MC-2"
            ),
            remove = { RuntimeException().left() }
        )

        val issue = mockIssue(
            listOf(DUPLICATES_LINK, link1, link2),
            listOf(DUPLICATES_LINK, link1, link2)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding a link fails" {
        val module = TransferLinksModule()

        val link = mockLink(
            key = "MC-42",
            issue = mockLinkedIssue(
                key = "MC-1",
                setField = { RuntimeException().left() }
            )
        )
        val issue = mockIssue(
            listOf(link, RELATES_LINK),
            listOf(link, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when adding multiple links fails" {
        val module = TransferLinksModule()

        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                setField = { RuntimeException().left() }
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            key = "MC-1",
            listOf(link, RELATES_LINK, RELATES_LINK),
            listOf(link, RELATES_LINK, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferLinksModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getField = { RuntimeException().left() }
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            listOf(link, RELATES_LINK),
            listOf(link, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferLinksModule()

        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getField = { RuntimeException().left() }
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            listOf(link, link, RELATES_LINK),
            listOf(link, link, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})

private fun linkIssues(
    key1: String,
    key2: String,
    type: String = "Duplicate",
    outwards: Boolean = true,
    createLink: (key1: String, type: String, key2: String) -> Unit = { _, _, _ -> Unit },
    removeLink: (key1: String, key2: String, type: String) -> Unit = { _, _, _ -> Unit }
) = mockLink(
    type = type,
    outwards = outwards,
    issue = mockLinkedIssue(
        key = key2,
        getFullIssue = {
            mockIssue(
                key = key2,
                links = listOf(
                    mockLink(
                        type = type,
                        outwards = !outwards,
                        issue = mockLinkedIssue(
                            key = key1,
                            createLink = { linkType, key -> createLink(key1, linkType, key).right() }
                        ),
                        remove = { removeLink(key2, type, key1).right() }
                    )
                ),
                createLink = { linkType, key -> createLink(key2, linkType, key).right() }
            ).right()
        },
        createLink = { linkType, key -> createLink(key2, linkType, key).right() }
    ),
    remove = { removeLink(key1, type, key2).right() }
)
