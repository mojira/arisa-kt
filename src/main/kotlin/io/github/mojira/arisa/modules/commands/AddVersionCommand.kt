package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class AddVersionCommand {
    operator fun invoke(issue: Issue, version: String): Int {
        if (issue.affectedVersions.any { it.name == version }) {
            throw CommandExceptions.VERSION_ALREADY_AFFECTED.create(version)
        }
        if (issue.project.versions.none { it.name == version }) {
            throw CommandExceptions.NO_SUCH_VERSION.create(version)
        }
        val id = issue.project.versions.first { it.name == version }.id
        issue.addAffectedVersionById(id)
        return 1
    }
}
