package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_BEFORE = RIGHT_NOW.minusSeconds(2)

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
    "should return OperationNotNeededModuleResponse when there was no change log" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there was no version changes" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
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

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is before last run" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Version",
                    created = RIGHT_NOW.minusSeconds(10),
                    changedTo = "3"
                )
            ),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version change is removing" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
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

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the future version is added by a staff" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
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

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are empty" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION)
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION)
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(FUTURE_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if no future version is marked affected" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(RELEASED_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if only an archived version is marked affected" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(ARCHIVED_VERSION),
            changeLog = listOf(ADD_ARCHIVED_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove future versions added by users" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
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

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeRight(ModuleResponse)
    }

    "should remove future versions added by users without a group" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
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

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when removing a version fails" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION_WITH_REMOVE_ERROR),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when removing versions fails" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION_WITH_REMOVE_ERROR, FUTURE_VERSION_WITH_REMOVE_ERROR),
            changeLog = listOf(ADD_FUTURE_VERSION, ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding the latest version fails" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION_WITH_ADD_ERROR)
            )
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding the comment fails" {
        val module = FutureVersionModule("message")
        val issue = mockIssue(
            affectedVersions = listOf(FUTURE_VERSION),
            changeLog = listOf(ADD_FUTURE_VERSION),
            project = mockProject(
                versions = listOf(RELEASED_VERSION)
            ),
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, TWO_SECONDS_BEFORE)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean,
    add: () -> Either<Throwable, Unit> = { Unit.right() },
    remove: () -> Either<Throwable, Unit> = { Unit.right() }
) = mockVersion(
    released = released,
    archived = archived,
    add = add,
    remove = remove
)
