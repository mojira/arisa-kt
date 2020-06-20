package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec

class AddVersionCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no argument is passed" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(true, false))
            )
        )

        val result = command(issue, "ARISA_ADD_VERSION")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when version is already added" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(true, false))
            ),
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_ADD_VERSION", "12w34a")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add version" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_ADD_VERSION", "12w34b")

        result.shouldBeRight(ModuleResponse)
    }

    "should add version with spaces" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(true, false),
                    getVersion(true, false, "Minecraft 12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(true, false))
        )

        val result = command(issue, "ARISA_ADD_VERSION", "Minecraft", "12w34b")

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
