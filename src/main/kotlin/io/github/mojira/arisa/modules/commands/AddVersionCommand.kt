package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.cloud.CloudIssue

class AddVersionCommand {
    operator fun invoke(issue: CloudIssue, version: String): Int {
        if (issue.affectedVersions.any { it.name == version }) {
            throw CommandExceptions.VERSION_ALREADY_AFFECTED.create(version)
        }
        val projectVersion = issue.project.versions.firstOrNull { it.name == version }
            ?: throw CommandExceptions.NO_SUCH_VERSION.create(version)

        val id = projectVersion.id

        // If version is archived, we need to unarchive it, add it, then re-archive
        if (projectVersion.archived) {
            issue.unarchiveVersion(id)
            issue.addAffectedVersionById(id)
            issue.archiveVersion(id)
        } else {
            // Normal case: just add the version
            issue.addAffectedVersionById(id)
        }

        return 1
    }
}
