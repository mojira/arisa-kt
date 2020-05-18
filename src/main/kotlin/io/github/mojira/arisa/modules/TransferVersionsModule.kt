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
            val oldestVersionOnParent = getOldestVersion(parentField)
            field
                .filter { it !in parentField }
                .filter { it isReleasedAfter oldestVersionOnParent }
                .map { it.name }
                .map(parentIssue.setField::partially1)
        }

    private fun getOldestVersion(field: List<Version>) = field
        .sortedBy { it.releaseDate }
        .getOrNull(0)

    private infix fun Version.isReleasedAfter(other: Version?) =
        other == null || this.releaseDate.isAfter(other.releaseDate)

    private fun LinkedIssue<*, *>.isSameProject(otherKey: String) =
        key.getProject() == otherKey.getProject()

    private fun LinkedIssue<*, *>.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
