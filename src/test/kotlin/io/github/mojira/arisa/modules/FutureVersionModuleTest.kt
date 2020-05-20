package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule("message")
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are empty" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(futureVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if no future version is marked affected" {
        val module = FutureVersionModule("message")
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(releasedVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if only an archived version is marked affected" {
        val module = FutureVersionModule("message")
        val archivedVersion = getVersion(false, true) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(archivedVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when removing a version fails" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { RuntimeException().left() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when removing versions fails" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { RuntimeException().left() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion, futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding the latest version fails" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { RuntimeException().left() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding the comment fails" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            ),
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getVersion(released: Boolean, archived: Boolean, remove: () -> Either<Throwable, Unit>) = mockVersion(
    released = released,
    archived = archived,
    remove = remove
)
