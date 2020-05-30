package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

/**
 * MC-42 duplicates MC-1.
 */
private val DUPLICATES_LINK = linkIssues(
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
            links = listOf(DUPLICATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing links to parent" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATES_LINK, RELATES_LINK)
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
                linkRemoved = true
                key1 shouldBe "MC-42"
                type shouldBe "Relates"
                key2 shouldBe "MC-10"
                Unit.right()
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATES_LINK, linkToTransfer)
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
                parentLinkRemoved = true
                Unit.right()
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
         * MC-42 duplicates MC-1.
         */
        val link = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = { _, _, _ ->
                parentLinkAdded = true
                Unit.right()
            }
        )

        /**
         * MC-42 is duplicated by MC-100.
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
                Unit.right()
            }
        )

        /**
         * MC-42 is duplicated by MC-101.
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
                Unit.right()
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

        /**
         * MC-42 is duplicated by MC-100.
         */
        val linkToTransfer = linkIssues(
            key1 = "MC-42",
            key2 = "MC-100",
            outwards = false,
            createLink = { key1, type, key2 ->
                when (key2) {
                    "MC-1" -> {
                        addedToFirstParent = true
                        key1 shouldBe "MC-100"
                        type shouldBe "Duplicate"
                    }
                    "MC-2" -> {
                        addedToSecondParent = true
                        key1 shouldBe "MC-100"
                        type shouldBe "Duplicate"
                    }
                    else -> fail("MC-100 should only duplicate MC-1 and MC-2")
                }
                Unit.right()
            }
        )

        /**
         * MC-42 duplicates MC-1.
         */
        val duplicatesLink1 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1"
        )

        /**
         * MC-42 duplicates MC-2.
         */
        val duplicatesLink2 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-2"
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink1, duplicatesLink2, linkToTransfer)
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

        /**
         * MC-42 relates to MC-10.
         */
        val outwardsRelates1 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-10",
            type = "Relates"
        )

        /**
         * MC-42 relates to MC-11.
         */
        val outwardsRelates2 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-11",
            type = "Relates"
        )

        /**
         * MC-42 duplicates MC-1.
         */
        val duplicatesLink = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = { key1, type, key2 ->
                secondLinkAdded = true
                when (key2) {
                    "MC-10" -> {
                        firstLinkAdded = true
                        key1 shouldBe "MC-1"
                        type shouldBe "Relates"
                    }
                    "MC-11" -> {
                        secondLinkAdded = true
                        key1 shouldBe "MC-1"
                        type shouldBe "Relates"
                    }
                    else -> fail("The parent should only be linked to MC-10 and MC-11")
                }
                Unit.right()
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink, outwardsRelates1, outwardsRelates2)
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

        /**
         * MC-42 relates to MC-10.
         */
        val outwardsRelates = linkIssues(
            key1 = "MC-42",
            key2 = "MC-10",
            type = "Relates"
        )

        /**
         * MC-42 duplicates MC-1.
         */
        val duplicatesLink1 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = { key1, type, key2 ->
                addedToFirstParent = true
                key1 shouldBe "MC-1"
                type shouldBe "Relates"
                key2 shouldBe "MC-10"
                Unit.right()
            }
        )

        /**
         * MC-42 duplicates MC-2.
         */
        val duplicatesLink2 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-2",
            createLink = { key1, type, key2 ->
                addedToSecondParent = true
                key1 shouldBe "MC-2"
                type shouldBe "Relates"
                key2 shouldBe "MC-10"
                Unit.right()
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink1, duplicatesLink2, outwardsRelates)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferLinksModule()

        /**
         * MC-42 duplicates MC-1.
         */
        val link = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            getFullIssue = {
                RuntimeException().left()
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(link, RELATES_LINK)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferLinksModule()

        /**
         * MC-42 duplicates MC-1.
         */
        val link = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            getFullIssue = {
                RuntimeException().left()
            }
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(link, link, RELATES_LINK)
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
    createLink: (key1: String, type: String, key2: String) -> Either<Throwable, Unit> = { _, _, _ -> Unit.right() },
    removeLink: (key1: String, key2: String, type: String) -> Either<Throwable, Unit> = { _, _, _ -> Unit.right() },
    getFullIssue: () -> Either<Throwable, Issue> = {
        mockIssue(
            key = key2,
            links = listOf(
                mockLink(
                    type = type,
                    outwards = !outwards,
                    issue = mockLinkedIssue(
                        key = key1,
                        createLink = { linkType, key -> createLink(key1, linkType, key) }
                    ),
                    remove = { removeLink(key2, type, key1) }
                )
            ),
            createLink = { linkType, key -> createLink(key2, linkType, key) }
        ).right()
    }
) = mockLink(
    type = type,
    outwards = outwards,
    issue = mockLinkedIssue(
        key = key2,
        getFullIssue = getFullIssue,
        createLink = { linkType, key -> createLink(key2, linkType, key) }
    ),
    remove = { removeLink(key1, type, key2) }
)
