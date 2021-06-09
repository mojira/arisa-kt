package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val FIVE_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private val VERSION = mockVersion(id = "1", released = true, archived = false)

private val EXTRA_VERSION = mockVersion(id = "2", released = true, archived = false)

private val ADD_VERSION = mockChangeLogItem(field = "Version", changedTo = "1", author = mockUser(name = "arisabot"))
private val VERSION_REMOVED =
    mockChangeLogItem(field = "Version", changedFrom = "1", author = mockUser("arisabot"))
private val VERSION_REMOVED_WITH_TO =
    mockChangeLogItem(field = "Version", changedFrom = "1", changedTo = "1", author = mockUser("arisabot"))

class RemoveVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no change log" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION, EXTRA_VERSION),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no version changes" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "description"
                )
            ),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is before last run" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    created = FIVE_SECONDS_AGO,
                    changedTo = "1"
                )
            ),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when only one version is present" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                ADD_VERSION
            ),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is an removal" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = null
                )
            ),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the future version is added by a volunteer" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(VERSION, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { listOf("helper") }
                )
            ),
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(VERSION, EXTRA_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove extra versions added via editing" {
        var removed = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(version, EXTRA_VERSION),
            changeLog = listOf(ADD_VERSION),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
    }

    "should set to nouser when user changes version 5 times" {
        var removed = false
        var reporterChanged = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(version, EXTRA_VERSION),
            changeLog = listOf(ADD_VERSION, VERSION_REMOVED, VERSION_REMOVED, VERSION_REMOVED, VERSION_REMOVED, VERSION_REMOVED),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            changeReporter = { reporterChanged = true },
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
        reporterChanged.shouldBeTrue()
    }

    "should not set to nouser when user changes version 4 times" {
        var removed = false
        var reporterChanged = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(version, EXTRA_VERSION),
            changeLog = listOf(ADD_VERSION, VERSION_REMOVED, VERSION_REMOVED, VERSION_REMOVED, VERSION_REMOVED),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            changeReporter = { reporterChanged = true },
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
        reporterChanged.shouldBeFalse()
    }

    "should not count to version for nouser" {
        var removed = false
        var reporterChanged = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(version, EXTRA_VERSION),
            changeLog = listOf(
                ADD_VERSION,
                VERSION_REMOVED,
                VERSION_REMOVED,
                VERSION_REMOVED,
                VERSION_REMOVED,
                VERSION_REMOVED_WITH_TO
            ),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            changeReporter = { reporterChanged = true },
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
        reporterChanged.shouldBeFalse()
    }

    "should remove extra versions added by users via editing" {
        var removed = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(version, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { listOf("user") }
                )
            ),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
    }

    "should remove extra versions added by users without a group via editing" {
        var removed = false
        val version = mockVersion(id = "1", released = true, archived = false)
        val module = RemoveVersionModule("removed-version")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(version, EXTRA_VERSION),
            resolution = "Invalid",
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "1",
                    getAuthorGroups = { null }
                )
            ),
            project = mockProject(
                versions = listOf(version, EXTRA_VERSION)
            ),
            removeAffectedVersion = { removed = it.id == "1" }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        removed.shouldBeTrue()
    }
})
