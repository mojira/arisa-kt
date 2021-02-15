package io.github.mojira.arisa.modules

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.LinkedIssue

class TransferVersionsModule : AbstractTransferFieldModule() {
    override fun filterParents(linkedIssue: LinkedIssue, issue: Issue): Boolean {
        return linkedIssue.isSameProject(issue) && linkedIssue.isUnresolved()
    }

    override fun getFunctions(parents: Collection<Issue>, issue: Issue): Collection<() -> Unit> =
        parents.flatMap { parent ->
            val parentVersionIds = parent.affectedVersions
                .map { it.id }

            issue.affectedVersions
                .map { it.id }
                .filter { it !in parentVersionIds }
                .map { { parent.addAffectedVersion(it) } }
        }

    private fun LinkedIssue.isSameProject(otherIssue: Issue) =
        key.getProject() == otherIssue.key.getProject()

    private fun LinkedIssue.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
