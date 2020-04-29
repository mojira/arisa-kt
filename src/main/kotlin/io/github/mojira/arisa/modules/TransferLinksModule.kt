package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.syntax.function.complement
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Link
import io.github.mojira.arisa.modules.TransferLinksModule.LinkParam

class TransferLinksModule : AbstractTransferFieldModule<List<Link<*, LinkParam>>, LinkParam>() {
    data class LinkParam(
        val type: String,
        val issue: String
    )

    override fun toFunction(
        parent: Pair<LinkedIssue<List<Link<*, LinkParam>>, LinkParam>, List<Link<*, LinkParam>>>,
        field: List<Link<*, LinkParam>>
    ): Collection<() -> Either<Throwable, Unit>> {
        val links = field
            .filter(::isDuplicatesLink.complement())

        val linkRemovers = links
            .map { it.remove }

        val linkAdders = links
            .filter(::parentDoesNotHaveLink.partially1(parent.second))
            .map(::toLinkAdder.partially2(parent))

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
            link.outwards -> link.issue.setField.partially1(LinkParam(link.type, parent.first.key))
            else -> parent.first.setField.partially1(LinkParam(link.type, link.issue.key))
        }
}