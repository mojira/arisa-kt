package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val VERSION_1 = getVersion(name = "v1", releaseDate = RIGHT_NOW.minusSeconds(300))
private val VERSION_2 = getVersion(name = "v2", releaseDate = RIGHT_NOW.minusSeconds(200))
private val VERSION_3 = getVersion(name = "v3", releaseDate = RIGHT_NOW.minusSeconds(100))
private val VERSION_X = getVersion(name = "vX", releaseDate = null)

class TransferVersionsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferVersionsModule()
        val issue = mockIssue(
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferVersionsModule()
        val link = mockLink(
            type = "Relates"
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferVersionsModule()
        val link = mockLink(
            outwards = false
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no affected versions" {
        val module = TransferVersionsModule()
        val link = mockLink()
        val issue = mockIssue(
            links = listOf(link)
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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent is from a different project" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MCL-1"
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version is released before the parent's oldest version (#229)" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        affectedVersions = listOf(VERSION_2)
                    ).right()
                }
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version's releaseDate is null (#250)" {
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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_X)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing versions to open parents" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1"
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should transfer missing versions to reopened parents" {
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                status = "Reopened"
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
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
                        addAffectedVersion = { v ->
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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )
        val result = module(issue, RIGHT_NOW)

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
                        addAffectedVersion = { v ->
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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1, VERSION_2)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        firstVersionAdded.shouldBeTrue()
        secondVersionAdded.shouldBeTrue()
    }

    "should only add versions released after the oldest version to parent (#229)" {
        var version1Added = false
        var version3Added = false
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersion = { v ->
                            when (v) {
                                "v1" -> version1Added = true
                                "v3" -> version3Added = true
                            }
                            Unit.right()
                        },
                        affectedVersions = listOf(VERSION_2)
                    ).right()
                }
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1, VERSION_3)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        version1Added.shouldBeFalse()
        version3Added.shouldBeTrue()
    }

    "should only add versions which has a non-null releaseDate (#250)" {
        var versionXAdded = false
        var version2Added = false
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersion = { v ->
                            when (v) {
                                "vx" -> versionXAdded = true
                                "v2" -> version2Added = true
                            }
                            Unit.right()
                        },
                        affectedVersions = listOf(VERSION_1)
                    ).right()
                }
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_X, VERSION_2)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        versionXAdded.shouldBeFalse()
        version2Added.shouldBeTrue()
    }

    "should only add versions released after the oldest version with a known releaseData to parent (#250)" {
        var version1Added = false
        var version3Added = false
        val module = TransferVersionsModule()
        val link = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersion = { v ->
                            when (v) {
                                "v1" -> version1Added = true
                                "v3" -> version3Added = true
                            }
                            Unit.right()
                        },
                        affectedVersions = listOf(VERSION_X, VERSION_2)
                    ).right()
                }
            )
        )
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1, VERSION_3)
        )
        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        version1Added.shouldBeFalse()
        version3Added.shouldBeTrue()
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
                        addAffectedVersion = {
                            addedToFirstParent = true
                            Unit.right()
                        }
                    ).right()
                }
            )
        )

        val link2 = mockLink(
            issue = mockLinkedIssue(
                key = "MC-1",
                getFullIssue = {
                    mockIssue(
                        addAffectedVersion = {
                            addedToSecondParent = true
                            Unit.right()
                        }
                    ).right()
                }
            )
        )

        val issue = mockIssue(
            links = listOf(link1, link2),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

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
        val issue = mockIssue(
            links = listOf(link),
            affectedVersions = listOf(VERSION_1)
        )

        val result = module(issue, RIGHT_NOW)

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
