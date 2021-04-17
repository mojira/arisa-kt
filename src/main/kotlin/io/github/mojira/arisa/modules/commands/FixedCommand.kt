package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixedCommand {
    @Suppress("ThrowsCount")
    operator fun invoke(issue: Issue, version: String): Int {
        if (issue.fixVersions.any { it.name == version }) {
            throw CommandExceptions.ALREADY_FIXED_IN.create(version)
        }
        val version = issue.project.versions.find { it.name == version }
        if (version == null) {
            throw CommandExceptions.NO_SUCH_VERSION.create(version)
        }
        if (issue.resolution !in listOf(null, "", "Unresolved")) {
            throw CommandExceptions.ALREADY_RESOLVED.create(issue.resolution)
        }
        issue.resolution = "Fixed"

        issue.addedFixVersions.add(version)
        return 1
    }
}
