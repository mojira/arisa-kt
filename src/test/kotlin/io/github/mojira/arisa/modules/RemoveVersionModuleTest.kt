package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val FIVE_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private val VERSION = mockVersion(id = "1", released = true, archived = false)

private val ADD_VERSION = mockChangeLogItem(field = "Version", changedTo = "1")

class RemoveVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no change log" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no version changes" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "description"
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is before last run" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    created = FIVE_SECONDS_AGO,
                    changedTo = "1"
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is an removal" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = null
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the future version is added by a volunteer" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { listOf("helper") }
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the ticket is unresolved" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
                created = FIVE_SECONDS_AGO,
                affectedVersions = listOf(VERSION),
                resolution = "Unresolved",
                changeLog = listOf(
                        mockChangeLogItem(
                                field = "Version",
                                changedTo = "1",
                                getAuthorGroups = { listOf("user") }
                        )
                ),
                project = mockProject(
                        versions = listOf(VERSION)
                )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove extra versions added via editing" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(VERSION),
            changeLog = listOf(ADD_VERSION),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove extra versions added by users via editing" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { listOf("user") }
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove extra versions added by users without a group via editing" {
        val module = RemoveVersionModule()
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { null }
                )
            ),
            project = mockProject(
                versions = listOf(VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }
})
