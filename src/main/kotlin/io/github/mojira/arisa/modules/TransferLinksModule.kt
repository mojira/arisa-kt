package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.syntax.function.complement
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link

class TransferLinksModule : AbstractTransferFieldModule() {
    override fun getFunctions(parents: Collection<Issue>, issue: Issue): Collection<() -> Either<Throwable, Unit>> {
        val links = issue.links
            .filter(::isDuplicatesLink.complement())

        val linkRemovers = links
            .map { it.remove }

        val linkAdders = parents
            .flatMap { parent ->
                links
                    .filter(::parentDoesNotHaveLink.partially1(parent.links))
                    .map(::toLinkAdder.partially2(parent))
            }

        return linkRemovers union linkAdders
    }

    private fun parentDoesNotHaveLink(parentLinks: List<Link>, other: Link) =
        parentLinks.none {
            it.type == other.type &&
                (it.type.toLowerCase() == "relates" || it.outwards == other.outwards) &&
                it.issue.key == other.issue.key
        }

    private fun toLinkAdder(
        link: Link,
        parent: Issue
    ) =
        when {
            link.outwards -> parent.createLink.partially1(link.type).partially1(link.issue.key)
            else -> link.issue.createLink.partially1(link.type).partially1(parent.key)
        }
}
