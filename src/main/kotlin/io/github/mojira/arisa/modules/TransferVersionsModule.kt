package io.github.mojira.arisa.modules

import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Version

class TransferVersionsModule : AbstractTransferFieldModule<List<Version>, String>() {
    override fun filterParents(issue: LinkedIssue<List<Version>, *>, request: Request<List<Version>, *>): Boolean {
        return issue.isSameProject(request.key) && issue.isUnresolved()
    }

    override fun getFunctions(
        parents: Collection<Pair<LinkedIssue<List<Version>, String>, List<Version>>>,
        field: List<Version>
    ) =
        parents.flatMap { (parentIssue, parentField) ->
            val oldestVersionWithKnownReleaseDateOnParent = getOldestVersionWithKnownReleaseDate(parentField)
            field
                .filter { it !in parentField }
                .filter { it isReleasedAfter oldestVersionWithKnownReleaseDateOnParent }
                .map { it.id }
                .map(parentIssue.setField::partially1)
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
                this.releaseDate?.isAfter(other.releaseDate) == true

    private fun LinkedIssue<*, *>.isSameProject(otherKey: String) =
        key.getProject() == otherKey.getProject()

    private fun LinkedIssue<*, *>.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
