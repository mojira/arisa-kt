package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Version

class TransferVersionsModule : AbstractTransferFieldModule() {
    override fun filterParents(linkedIssue: LinkedIssue, issue: Issue): Boolean {
        return linkedIssue.isSameProject(issue) && linkedIssue.isUnresolved()
    }

    override fun getFunctions(parents: Collection<Issue>, issue: Issue): Collection<() -> Either<Throwable, Unit>> =

        parents.flatMap { parent ->
            val parentVersionIds = parent.affectedVersions
                .map { it.id }

            val oldestVersionWithKnownReleaseDateOnParent =
                getOldestVersionWithKnownReleaseDate(parent.affectedVersions)

            issue.affectedVersions
                .filter { it isReleasedAfter oldestVersionWithKnownReleaseDateOnParent }
                .map { it.id }
                .filter { it !in parentVersionIds }
                .map { { parent.addAffectedVersion(it).right() } }
        }

    private fun getOldestVersionWithKnownReleaseDate(field: List<Version>) = field
        .filter { it.releaseDate != null }
        .sortedBy { it.releaseDate }
        .getOrNull(0)

    /**
     * Returns true only if:
     * - The other version is `null`;
     * - The other version has a `null` release date; OR
     * - The current version has a non-`null` release date which is after the other version's release date.
     */
    private infix fun Version.isReleasedAfter(other: Version?) =
        other?.releaseDate == null ||
                (releaseDate != null && releaseDate.isAfter(other.releaseDate))

    private fun LinkedIssue.isSameProject(otherIssue: Issue) =
        key.getProject() == otherIssue.key.getProject()

    private fun LinkedIssue.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
