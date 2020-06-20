package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class FixedCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no argument is passed" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(true, false))
            )
        )

        val result = command(issue, "ARISA_FIXED")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add version" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_FIXED", "12w34b")

        result.shouldBeRight(ModuleResponse)
    }

    "should add version with spaces" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "Minecraft 12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_FIXED", "Minecraft", "12w34b")

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when adding a version fails" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "12w34b", add = { RuntimeException().left() })
                )
            ),
            markAsFixedInASpecificVersion = { _ -> RuntimeException().left() },
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_FIXED", "12w34b")

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean,
    name: String = "12w34a",
    add: () -> Either<Throwable, Unit> = { Unit.right() },
    remove: () -> Either<Throwable, Unit> = { Unit.right() }
) = mockVersion(
    name = name,
    released = released,
    archived = archived,
    add = add,
    remove = remove
)
