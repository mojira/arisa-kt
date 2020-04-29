package io.github.mojira.arisa.modules

import arrow.syntax.function.partially1

class TransferVersionsModule : AbstractTransferFieldModule<List<String>>() {
    override fun filterParents(issue: LinkedIssue<List<String>>, request: Request<List<String>>): Boolean {
        return issue.isSameProject(request.key) && issue.isUnresolved()
    }

    override fun toFunctions(parent: Pair<LinkedIssue<List<String>>, List<String>>, field: List<String>) =
        field
            .filter { it !in parent.second }
            .map { parent.first.transferFieldTo.partially1(it) }

    private fun LinkedIssue<*>.isSameProject(otherKey: String) =
        key.getProject() == otherKey.getProject()

    private fun LinkedIssue<*>.isUnresolved() =
        status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))
}
