package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class AddVersionCommand : Command1<String> {
    override operator fun invoke(issue: Issue, arg: String): Int {
        if (issue.affectedVersions.any { it.name == arg }) {
            throw CommandExceptions.VERSION_ALREADY_AFFECTED.create(arg)
        }
        if (issue.project.versions.none { it.name == arg }) {
            throw CommandExceptions.NO_SUCH_VERSION.create(arg)
        }
        val id = issue.project.versions.first { it.name == arg }.id
        issue.addAffectedVersion(id)
        return 1
    }
}
