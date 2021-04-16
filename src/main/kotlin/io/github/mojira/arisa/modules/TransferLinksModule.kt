package io.github.mojira.arisa.modules

import arrow.syntax.function.complement
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link

class TransferLinksModule : AbstractTransferFieldModule() {
    override fun getFunctions(parents: Collection<Issue>, issue: Issue): Collection<() -> Unit> {
        val links = issue.links
            .filter(::isDuplicatesLink.complement())

        val linkRemovers = links
            .map { { issue.removedLinks.add(it); Unit } }

        val linkAdders = parents
            .flatMap { parent ->
                links
                    .filter(::parentDoesNotHaveLink.partially1(parent.links))
                    .filter(::doesNotLinkToParent.partially1(parent.key))
                    .map(::toLinkAdder.partially2(parent))
            }

        return linkRemovers union linkAdders
    }

    private fun parentDoesNotHaveLink(parentLinks: List<Link>, other: Link) =
        parentLinks.none {
            it.type == other.type &&
                    (it.type.toLowerCase() == "relates" || it.outwards == other.outwards) &&
                    it.issue?.key == other.issue?.key
        }

    private fun doesNotLinkToParent(key: String, link: Link) = key != link.issue?.key

    private fun toLinkAdder(
        link: Link,
        parent: Issue
    ): () -> Unit = {
        if (link.outwards) {
            parent.addLink(link.type, link.outwards, link.issue!!.key)
        } else {
            parent.addLink(link.type, !link.outwards, link.issue!!.key)
        }
    }
}
