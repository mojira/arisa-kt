package io.github.mojira.arisa.modules

import arrow.core.left
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockUser
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val FIVE_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private val ARCHIVED_VERSION = mockVersion(id = "1", released = false, archived = true)
private val RELEASED_VERSION = mockVersion(id = "2", released = true, archived = false)
private val FUTURE_VERSION = mockVersion(id = "3", released = false, archived = false)
private val RELEASED_VERSION_WITH_ADD_ERROR = mockVersion(
    id = "2", released = true, archived = false,
    add = { RuntimeException().left() }
)
private val FUTURE_VERSION_WITH_REMOVE_ERROR = mockVersion(
    id = "3", released = false, archived = false,
    remove = { RuntimeException().left() }
)

private val ADD_ARCHIVED_VERSION = mockChangeLogItem(field = "Version", changedTo = "1")
private val ADD_FUTURE_VERSION = mockChangeLogItem(field = "Version", changedTo = "3")

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no change log" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no version changes" {
        val module = FutureVersionModule("messgit age", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "description"
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is before last run" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    created = FIVE_SECONDS_AGO,
                    changedTo = "3"
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is an removal" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = null
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the future version is added by a staff upon ticket creation" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            reporter = mockUser(
                getGroups = { listOf("staff") }
            ),
            affectedVersions = listOf(FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the future version is added by a staff via editing" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "3",
                    getAuthorGroups = { listOf("staff") }
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are empty" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION)
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(FUTURE_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if no future version is marked affected" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(RELEASED_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if only an archived version is marked affected" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(ARCHIVED_VERSION),
            changeLog = listOf(ADD_ARCHIVED_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions added upon ticket creation" {
        var isResolved = false

        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            ),
            resolveAsAwaitingResponse = { isResolved = true }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        isResolved.shouldBeTrue()
    }

    "should remove future versions added upon ticket creation by users" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            reporter = mockUser(
                getGroups = { listOf("user") }
            ),
            affectedVersions = listOf(FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should not resolve if already resolved" {
        var preResolved = true

        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            resolution = "Invalid",
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            ),
            resolveAsAwaitingResponse = { preResolved = false }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        preResolved.shouldBeTrue()
    }

    "should remove future versions added via editing" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should not resolve if there is another version" {
        var isResolved = false

        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION, RELEASED_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            ),
            resolveAsAwaitingResponse = { isResolved = true }
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        isResolved.shouldBeFalse()
    }

    "should remove future versions added by users via editing" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "3",
                    getAuthorGroups = { listOf("user") }
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove future versions added by users without a group via editing" {
        val module = FutureVersionModule("message", "panel")
        val issue = mockIssue(
            created = FIVE_SECONDS_AGO,
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    changedTo = "3",
                    getAuthorGroups = { null }
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }
})
