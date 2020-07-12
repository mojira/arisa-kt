package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
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

private val MC_1_LINK_CHANGELOG = mockChangeLogItem(
    created = RIGHT_NOW,
    changedTo = "MC-1"
)

private val MC_10_LINK_CHANGELOG = mockChangeLogItem(
    created = RIGHT_NOW,
    changedTo = "MC-10"
)
private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class TransferLinksModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(RELATES_LINK),
            changeLog = listOf(MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferLinksModule()
        val link = linkIssues(
            key1 = "MC-1",
            key2 = "MC-42",
            outwards = false
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-42"
        )
        val issue = mockIssue(
            key = "MC-1",
            links = listOf(link),
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no additional links" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no new parent links" {
        val module = TransferLinksModule()

        val oldDuplicatesLinkChangeLog = mockChangeLogItem(
            created = A_SECOND_AGO,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATES_LINK, RELATES_LINK),
            changeLog = listOf(oldDuplicatesLinkChangeLog, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing links to parent" {
        val module = TransferLinksModule()
        val issue = mockIssue(
            key = "MC-42",
            links = listOf(DUPLICATES_LINK, RELATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should not transfer relates link that exists in parent regardless of direction" {
        var hasTransferred = false
        val module = TransferLinksModule()

        val createLink = { _: String, _: String, _: String -> hasTransferred = true; Unit.right() }

        /**
         * MC-42 duplicates MC-1
         */
        val link = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = createLink,
            additionalLinksOnParent = listOf(
                /**
                 * MC-10 relates to MC-1
                 */
                linkIssues(
                    key1 = "MC-1",
                    key2 = "MC-10",
                    type = "Relates",
                    outwards = false,
                    createLink = createLink
                )
            )
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(link, RELATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasTransferred.shouldBeFalse()
    }

    "should not transfer links that link to the parent" {
        var hasTransferred = false
        val module = TransferLinksModule()

        val createLink = { _: String, _: String, _: String -> hasTransferred = true; Unit.right() }

        /**
         * MC-42 duplicates MC-1
         */
        val duplicatesLink = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            createLink = createLink
        )

        /**
         * MC-42 relates to MC-1
         */
        val relatesLink = linkIssues(
            key1 = "MC-42",
            key2 = "MC-1",
            type = "Relates"
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink, relatesLink),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_1_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasTransferred.shouldBeFalse()
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
            links = listOf(DUPLICATES_LINK, linkToTransfer),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

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
            links = listOf(duplicatesLink, RELATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

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
        
        val mc100LinkChangeLog = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-100"
        )

        val mc101LinkChangeLog = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-101"
        )

        val issue = mockIssue(
            links = listOf(link, link1, link2),
            changeLog = listOf(MC_1_LINK_CHANGELOG, mc100LinkChangeLog, mc101LinkChangeLog)
        )
        val result = module(issue, A_SECOND_AGO)

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
                    "MC-10" -> {
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
         * MC-42 duplicates MC-10.
         */
        val duplicatesLink2 = linkIssues(
            key1 = "MC-42",
            key2 = "MC-10"
        )

        val mc100LinkChangeLog = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-100"
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink1, duplicatesLink2, linkToTransfer),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG, mc100LinkChangeLog)
        )

        val result = module(issue, A_SECOND_AGO)

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

        val mc11LinkChangeLog = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-11"
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink, outwardsRelates1, outwardsRelates2),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG, mc11LinkChangeLog)
        )
        val result = module(issue, A_SECOND_AGO)

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

        val mc2LinkChangeLog = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-2"
        )

        val issue = mockIssue(
            key = "MC-42",
            links = listOf(duplicatesLink1, duplicatesLink2, outwardsRelates),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG, mc2LinkChangeLog)
        )

        val result = module(issue, A_SECOND_AGO)

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
            links = listOf(link, RELATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

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
            links = listOf(link, link, RELATES_LINK),
            changeLog = listOf(MC_1_LINK_CHANGELOG, MC_1_LINK_CHANGELOG, MC_10_LINK_CHANGELOG)
        )

        val result = module(issue, A_SECOND_AGO)

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
    additionalLinksOnParent: List<Link> = emptyList(),
    getFullIssue: () -> Either<Throwable, Issue> = {
        mockIssue(
            key = key2,
            links = mutableListOf(
                mockLink(
                    type = type,
                    outwards = !outwards,
                    issue = mockLinkedIssue(
                        key = key1,
                        createLink = { linkType, key -> createLink(key1, linkType, key) }
                    ),
                    remove = { removeLink(key2, type, key1) }
                )
            ).also { it.addAll(additionalLinksOnParent) },
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
