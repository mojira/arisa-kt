package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import java.time.Instant

private val NOW = Instant.now()

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule("message")
        val releasedVersion = getVersion(true, false)
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
        val futureVersion = getVersion(false, false)
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false)
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule("message")
        val futureVersion = getVersion(false, false)
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
        val releasedVersion = getVersion(true, false)
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
        val archivedVersion = getVersion(false, true)
        val releasedVersion = getVersion(true, false)
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
        val futureVersion = getVersion(false, false)
        val releasedVersion = getVersion(true, false)
        val issue = mockIssue(
            affectedVersions = listOf(futureVersion),
            project = mockProject(
                versions = listOf(releasedVersion)
            )
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean
) = mockVersion(
    released = released,
    archived = archived
)
