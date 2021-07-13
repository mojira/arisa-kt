package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixedCommand {
    @Suppress("ThrowsCount")
    operator fun invoke(issue: Issue, version: String): Int {
        if (issue.fixVersions.any { it.name == version }) {
            throw CommandExceptions.ALREADY_FIXED_IN.create(version)
        }
        if (issue.project.versions.none { it.name == version }) {
            throw CommandExceptions.NO_SUCH_VERSION.create(version)
        }
        if (issue.resolution !in listOf(null, "", "Unresolved")) {
            throw CommandExceptions.ALREADY_RESOLVED.create(issue.resolution)
        }
        if (issue.affectedVersions.any { it.releaseDate!!.isAfter(issue.project.versions.first { v -> v.name == version }.releaseDate) }) {
            throw CommandExceptions.FIX_VERSION_BEFORE_LATEST_AFFECTED_VERSION.create(version)
        }

        issue.markAsFixedWithSpecificVersion(version)
        return 1
    }
}
