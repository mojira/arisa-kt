package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class FixedCommand {
    @Suppress("ThrowsCount")
    operator fun invoke(issue: Issue, fixVersionName: String, force: Boolean): Int {
        if (issue.fixVersions.any { it.name == fixVersionName }) {
            throw CommandExceptions.ALREADY_FIXED_IN.create(fixVersionName)
        }

        val fixVersion = issue.project.versions.firstOrNull { it.name == fixVersionName }
            ?: throw CommandExceptions.NO_SUCH_VERSION.create(fixVersionName)

        if (issue.resolution !in listOf(null, "", "Unresolved")) {
            throw CommandExceptions.ALREADY_RESOLVED.create(issue.resolution)
        }

        if (!force && fixVersion.releaseDate != null) {
            // Fail if any affected version is same or newer than fix version
            // Since archived fix versions cannot be removed again this prevents accidentally adding an incorrect
            // fix version
            issue.affectedVersions.firstOrNull { it.releaseDate?.isSameOrAfter(fixVersion.releaseDate) == true }?.let {
                throw CommandExceptions.FIX_VERSION_SAME_OR_BEFORE_AFFECTED_VERSION.create(fixVersionName, it.name)
            }
        }

        issue.markAsFixedWithSpecificVersion(fixVersionName)
        return 1
    }
}

private fun Instant.isSameOrAfter(other: Instant) = !this.isBefore(other)
