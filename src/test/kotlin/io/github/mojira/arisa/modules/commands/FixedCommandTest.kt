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
        var fixVersion: String? = null

        val affectedVersion1 = getVersion(released = true, archived = false, "12w34a", releaseDate = TWO_YEARS_AGO)
        val affectedVersion2 = getVersion(released = true, archived = false, "12w34b", releaseDate = TEN_SECONDS_AGO)
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    affectedVersion1,
                    affectedVersion2
                )
            ),
            affectedVersions = listOf(affectedVersion1),
            markAsFixedInASpecificVersion = { fixVersion = it }
        )

        val result = command(issue, affectedVersion2.name, false)
        result shouldBe 1
        fixVersion shouldBe affectedVersion2.name
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
            command(issue, "12w34b", false)
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
            command(issue, "12w34b", false)
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
            command(issue, "12w34b", false)
        }
        exception.message shouldBe "The ticket was already resolved as Cannot Reproduce"
    }

    "should throw FIX_VERSION_SAME_OR_BEFORE_AFFECTED_VERSION when the fix version is same as an affected version" {
        val command = FixedCommand()

        val affectedVersion = getVersion(released = true, archived = false, "12w34a")
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    affectedVersion
                )
            ),
            affectedVersions = listOf(affectedVersion)
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, affectedVersion.name, false)
        }
        exception.message shouldBe "Cannot add fix version 12w34a because the affected version 12w34a of the issue " +
                "is the same or was released after it; run with `<version> force` to add the fix version anyways"
    }

    "should throw FIX_VERSION_SAME_OR_BEFORE_AFFECTED_VERSION when the fix version is before an affected version" {
        val command = FixedCommand()

        val affectedVersion1 = getVersion(released = true, archived = false, "12w34a", releaseDate = TWO_YEARS_AGO)
        val affectedVersion2 = getVersion(released = true, archived = false, "12w34b", releaseDate = TEN_SECONDS_AGO)
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    affectedVersion1,
                    affectedVersion2
                )
            ),
            affectedVersions = listOf(affectedVersion2)
        )

        val exception = shouldThrow<CommandSyntaxException> {
            command(issue, affectedVersion1.name, false)
        }
        exception.message shouldBe "Cannot add fix version 12w34a because the affected version 12w34b of the issue " +
                "is the same or was released after it; run with `<version> force` to add the fix version anyways"
    }

    "should not throw when the fix version is same as an affected version, but 'force' is used" {
        val command = FixedCommand()
        var fixVersion: String? = null

        val affectedVersion = getVersion(released = true, archived = false, "12w34a")
        val issue = mockIssue(
            project = mockProject(
                versions = listOf(
                    affectedVersion
                )
            ),
            affectedVersions = listOf(affectedVersion),
            markAsFixedInASpecificVersion = { fixVersion = it }
        )

        val result = command(issue, affectedVersion.name, true)
        result shouldBe 1
        fixVersion shouldBe affectedVersion.name
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
