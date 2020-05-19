package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Version

class TransferVersionsModule : AbstractTransferFieldModule() {
    override fun filterParents(linkedIssue: LinkedIssue, issue: Issue): Boolean {
        return linkedIssue.isSameProject(issue) && linkedIssue.isUnresolved()
    }

    override fun getFunctions(
        parents: Collection<Pair<LinkedIssue, Issue>>,
        issue: Issue
    ): Collection<() -> Either<Throwable, Unit>> =

        parents.flatMap { parent ->
            val parentVersionIds = parent.second.affectedVersions
                .map { it.id }

            val oldestVersionOnParent = getOldestVersion(parent.second.affectedVersions)

            issue.affectedVersions
                .filter { it isReleasedAfter oldestVersionOnParent }
                .map { it.id }
                .filter { it !in parentVersionIds }
                .map(parent.first.addAffectedVersion::partially1)
        }

    private fun getOldestVersion(field: List<Version>) = field
        .sortedBy { it.releaseDate }
        .getOrNull(0)

    private infix fun Version.isReleasedAfter(other: Version?) =
        other == null || this.releaseDate.isAfter(other.releaseDate)

    private fun LinkedIssue.isSameProject(otherIssue: Issue) =
        key.getProject() == otherIssue.key.getProject()

    private fun LinkedIssue.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
