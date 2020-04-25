package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2

class TransferVersionsModule : Module<TransferVersionsModule.Request> {
    data class LinkedIssue(
        val key: String,
        val status: String,
        val addVersion: (key: String) -> Either<Throwable, Unit>,
        val getAffectedVersions: () -> Either<Throwable, List<String>>
    )

    data class Link(
        val type: String,
        val outwards: Boolean,
        val issue: LinkedIssue
    )

    data class Request(
        val key: String,
        val links: List<Link>,
        val versions: List<String>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val relevantParents = links
                .filter(::isDuplicatesLink)
                .map { it.issue }
                .filter(::isSameProject.partially2(key))
                .filter(::isUnresolved)
            assertGreaterThan(relevantParents.size, 0).bind()

            val parentEithers = relevantParents
                .map(::toIssueVersionPair)

            parentEithers.toFailedModuleEither().bind()
            val parents = parentEithers
                .map { (it as Either.Right).b }

            val versionAdders = parents
                .flatMap(::toVersionAdders.partially2(versions))

            assertNotEmpty(versionAdders).bind()
            tryRunAll(versionAdders).bind()
        }
    }

    private fun toIssueVersionPair(issue: LinkedIssue): Either<Throwable, Pair<LinkedIssue, List<String>>> {
        val details = issue.getAffectedVersions()
        return details.fold(
            { it.left() },
            { (issue to it).right() }
        )
    }

    private fun toVersionAdders(parent: Pair<LinkedIssue, List<String>>, versions: List<String>) =
        versions
            .filter { it !in parent.second }
            .map{ parent.first.addVersion.partially1(it) }

    private fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards

    private fun isSameProject(issue:  LinkedIssue, key: String) =
        issue.key.getProject() == key.getProject()

    private fun isUnresolved(issue: LinkedIssue) =
        issue.status in listOf("Open", "Reopened")

    private fun String.getProject() =
        substring(0, indexOf('-'))

}
