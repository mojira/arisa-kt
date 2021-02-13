package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixedCommand : Command1<String> {
    override operator fun invoke(issue: Issue, arg: String): Int {
        if (issue.fixVersions.any { it.name == arg }) {
            throw CommandExceptions.ALREADY_FIXED_IN.create(arg)
        }
        if (issue.project.versions.none { it.name == arg }) {
            throw CommandExceptions.NO_SUCH_VERSION.create(arg)
        }
        if (issue.resolution !in listOf(null, "", "Unresolved")) {
            throw CommandExceptions.ALREADY_RESOLVED.create(issue.resolution)
        }
        issue.markAsFixedWithSpecificVersion(arg)
        return 1
    }
}
