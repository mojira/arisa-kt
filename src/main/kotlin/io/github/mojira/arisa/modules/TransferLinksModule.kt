package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.syntax.function.complement
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkParam
import io.github.mojira.arisa.domain.LinkedIssue

class TransferLinksModule : AbstractTransferFieldModule<List<Link<*, LinkParam>>, LinkParam>() {
    override fun getFunctions(
        parents: Collection<Pair<LinkedIssue<List<Link<*, LinkParam>>, LinkParam>, List<Link<*, LinkParam>>>>,
        field: List<Link<*, LinkParam>>
    ): Collection<() -> Either<Throwable, Unit>> {
        val links = field
            .filter(::isDuplicatesLink.complement())

        val linkRemovers = links
            .map { it.remove }

        val linkAdders = parents
            .flatMap { parent ->
                links
                    .filter(::parentDoesNotHaveLink.partially1(parent.second))
                    .map(::toLinkAdder.partially2(parent))
            }

        return linkRemovers union linkAdders
    }

    private fun parentDoesNotHaveLink(parentLinks: List<Link<*, *>>, other: Link<*, *>) =
        parentLinks.none {
            it.type == other.type &&
                it.outwards == other.outwards &&
                it.issue.key == other.issue.key
        }

    private fun toLinkAdder(
        link: Link<*, LinkParam>,
        parent: Pair<LinkedIssue<*, LinkParam>, List<Link<*, LinkParam>>>
    ) =
        when {
            link.outwards -> parent.first.setField.partially1(LinkParam(link.type, link.issue.key))
            else -> link.issue.setField.partially1(LinkParam(link.type, parent.first.key))
        }
}
