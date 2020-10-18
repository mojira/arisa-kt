package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixedCommand {
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
        issue.markAsFixedWithSpecificVersion(version)
        return 1
    }
}
