package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val VERSION_1 = getVersion(name = "v1")
private val VERSION_2 = getVersion(name = "v2")
private val VERSION_X = getVersion(name = "vX")
private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class TransferVersionsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferVersionsModule()
        val issue = mockIssue(
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferVersionsModule()
        val link = mockLink(
            type = "Relates"
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferVersionsModule()
        val link = mockLink(
            outwards = false
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent is resolved" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                status = "Resolved"
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no affected versions" {
        val module = TransferVersionsModule()
        val link = mockLink()
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no link was added since last run" {
        val module = TransferVersionsModule()
        val link = mockLink()
        val changeLogItem = mockChangeLogItem(
            created = A_SECOND_AGO,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent already has all versions" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        affectedVersions = listOf(VERSION_1)
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent is from a different project" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MCL-1"
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing versions to open parents" {
        val module = TransferVersionsModule()
        val addedVersions = mutableListOf<String>()

        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        affectedVersions = listOf(VERSION_1),
                        addAffectedVersionById = { addedVersions.add(it) }
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        // Note: VERSION_1 is already affected, should not be added
        addedVersions shouldContainExactly listOf(VERSION_2.id)
    }

    "should transfer missing versions to reopened parents" {
        val module = TransferVersionsModule()
        val addedVersions = mutableListOf<String>()

        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                status = "Reopened",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = { addedVersions.add(it) }
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        addedVersions shouldContainExactly listOf(VERSION_1.id)
    }

    "should not transfer versions listed in notTransferredVersionIds" {
        // Don't transfer VERSION_2
        val module = TransferVersionsModule(listOf(VERSION_2.id))
        val addedVersions = mutableListOf<String>()

        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = { addedVersions.add(it) }
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        addedVersions shouldContainExactly listOf(VERSION_1.id)
    }

    "should add all versions to parent" {
        var firstVersionAdded = false
        var secondVersionAdded = false
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = { v ->
                            when (v) {
                                "v1" -> firstVersionAdded = true
                                "v2" -> secondVersionAdded = true
                            }
                            Unit.right()
                        }
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )
        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        firstVersionAdded.shouldBeTrue()
        secondVersionAdded.shouldBeTrue()
    }

    "should add all versions to parent if the only version it has has a null releaseDate (#250)" {
        var firstVersionAdded = false
        var secondVersionAdded = false
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = { v ->
                            when (v) {
                                "v1" -> firstVersionAdded = true
                                "v2" -> secondVersionAdded = true
                            }
                            Unit.right()
                        },
                        affectedVersions = listOf(VERSION_X)
                    ).right()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )
        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        firstVersionAdded.shouldBeTrue()
        secondVersionAdded.shouldBeTrue()
    }

    "should add versions to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferVersionsModule()
        val link1 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = {
                            addedToFirstParent = true
                            Unit.right()
                        }
                    ).right()
                }
            )
        )

        val link2 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-2",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersionById = {
                            addedToSecondParent = true
                            Unit.right()
                        }
                    ).right()
                }
            )
        )
        val changeLogItem1 = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val changeLogItem2 = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-2"
        )

        val issue = mockIssue(
            links = listOf(link1, link2),
            changeLog = listOf(changeLogItem1, changeLogItem2),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    RuntimeException().left()
                }
            )
        )
        val changeLogItem = mockChangeLogItem(
            created = RIGHT_NOW,
            changedTo = "MC-1"
        )
        val issue = mockIssue(
            links = listOf(link),
            changeLog = listOf(changeLogItem),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getVersion(name: String, releaseDate: Instant? = RIGHT_NOW) = mockVersion(
    id = name,
    released = true,
    archived = false,
    releaseDate = releaseDate
)
