package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AddVersionCommandTest : StringSpec({
    "should throw VERSION_ALREADY_AFFECTED when version is already added" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(released = true, archived = false))
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false))
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34a")
        }
        exception.message shouldBe "The version 12w34a was already marked as affected"
    }

    "should throw NO_SUCH_VERSION when version doesn't exist" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(getVersion(released = true, archived = false))
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false))
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34b")
        }
        exception.message shouldBe "The version 12w34b doesn't exist in this project"
    }

    "should add version" {
        val command = AddVersionCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(released = true, archived = false),
                    getVersion(released = true, archived = false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false))
        )

        val result = command(issue, "12w34b")

        result shouldBe 1
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean,
    name: String = "12w34a",
    add: () -> Unit = { },
    remove: () -> Unit = { }
) = mockVersion(
    name = name,
    released = released,
    archived = archived,
    add = add,
    remove = remove
)
