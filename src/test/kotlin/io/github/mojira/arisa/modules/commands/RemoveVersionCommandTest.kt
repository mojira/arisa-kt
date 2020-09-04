package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec

class RemoveVersionCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no argument is passed" {
        val command = RemoveVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(true, false))
            )
        )

        val result = command(issue, "ARISA_REMOVE_VERSION")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when version is not added" {
        val command = RemoveVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                        getVersion(true, false, "1.15.3"),
                        getVersion(true, false)
                )
            ),
            affectedVersions = listOf(getVersion(true, false, "1.15.3"))
        )

        val result = command(issue, "ARISA_REMOVE_VERSION", "12w34a")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove version" {
        val command = RemoveVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false, "12w34b"))
        )

        val result = command(issue, "ARISA_REMOVE_VERSION", "12w34b")

        result.shouldBeRight(ModuleResponse)
    }

    "should remove version with spaces" {
        val command = RemoveVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "Minecraft 12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false, "Minecraft 12w34b"))
        )

        val result = command(issue, "ARISA_REMOVE_VERSION", "Minecraft", "12w34b")

        result.shouldBeRight(ModuleResponse)
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean,
    name: String = "12w34a",
    add: () -> Unit = { Unit },
    remove: () -> Unit = { Unit }
) = mockVersion(
    name = name,
    released = released,
    archived = archived,
    add = add,
    remove = remove
)
