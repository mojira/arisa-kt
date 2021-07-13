package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.github.mojira.arisa.utils.mockVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

private val TEN_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)
private val TWO_YEARS_AGO = RIGHT_NOW.minus(730, ChronoUnit.DAYS)

class FixedCommandTest : StringSpec({
    "should add version" {
        val command = FixedCommand()

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

    "should throw ALREADY_FIXED_IN when ticket is already fixed in such version" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(released = true, archived = false),
                    getVersion(released = true, archived = false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false)),
            fixVersions = listOf(getVersion(released = true, archived = false, "12w34b"))
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34b")
        }
        exception.message shouldBe "The ticket was already marked as fixed in 12w34b"
    }

    "should throw NO_SUCH_VERSION when version doesn't exist" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(released = true, archived = false)
                )
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false))
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34b")
        }
        exception.message shouldBe "The version 12w34b doesn't exist in this project"
    }

    "should throw ALREADY_RESOLVED when already resolved" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(released = true, archived = false),
                    getVersion(released = true, archived = false, "12w34b")
                )
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false)),
            resolution = "Cannot Reproduce"
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34b")
        }
        exception.message shouldBe "The ticket was already resolved as Cannot Reproduce"
    }

    "should throw FIX_VERSION_BEFORE_LATEST_AFFECTED_VERSION when the fix version was released before one of the affected versions" {
        val command = FixedCommand()

        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    getVersion(released = true, archived = false, "12w34a", releaseDate = TWO_YEARS_AGO),
                    getVersion(released = true, archived = false, "12w34b", releaseDate = TEN_SECONDS_AGO)
                )
            ),
            affectedVersions = listOf(getVersion(released = true, archived = false, "12w34b", releaseDate = TEN_SECONDS_AGO))
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, "12w34a")
        }
        exception.message shouldBe "Cannot add fix version 12w34a because a newer version is marked as affecting the issue"
    }
})

private fun getVersion(
    released: Boolean,
    archived: Boolean,
    name: String = "12w34a",
    releaseDate: Instant = RIGHT_NOW
) = mockVersion(
    name = name,
    released = released,
    archived = archived,
    releaseDate = releaseDate
)
