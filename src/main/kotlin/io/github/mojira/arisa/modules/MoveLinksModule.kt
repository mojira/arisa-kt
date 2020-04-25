package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.complement
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.modules.MoveLinksModule.Request

class MoveLinksModule : Module<Request> {
    data class LinkedIssue(
        val links: List<Link>,
        val addLink: (type: String, key: String) -> Either<Throwable, Unit>
    )

    data class Link(
        val type: String,
        val inwards: Boolean,
        val linkedKey: String,
        val getLinkedIssueDetails: () -> Either<Throwable, LinkedIssue>,
        val addLinkToLinkedIssue: (type: String, key: String) -> Either<Throwable, Unit>,
        val remove: () -> Either<Throwable, Unit>
    )

    data class Request(
        val links: List<Link>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val parentLinks = links.filter(::isDuplicatesLink)
            assertGreaterThan(parentLinks.size, 0).bind()
            val parents = parentLinks
                .map { it.linkedKey to it.getLinkedIssue() }

            val linksToBeTransferred = links
                .filter(::isDuplicatesLink.complement())
            assertNotEmpty(linksToBeTransferred).bind()

            val functions = linksToBeTransferred
                .map { it.remove } union
                    linksToBeTransferred
                        .flatMap(::applyParents.partially2(parents))
                        .filterNotNull()



            val linksToTransfer = links.filter(::isDuplicatesLink.complement())
            val linkRemovers = linksToTransfer.map{ it.remove }
            val linkAdders = parents
                .flatMap(::toLinkAdders.partially2(linksToTransfer))

            tryRunAll(functions).bind()
        }
    }

    private fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && !link.inwards

    private fun applyParents(link: Link, parents: List<Pair<String, Either<Throwable, LinkedIssue>>>) =
        parents.map { (parentKey, parentEither) ->
            parentEither.fold(
                { { it.left() } },
                { parent ->
                    when {
                        parent.links.any(::equal.partially1(link)) -> null
                        link.inwards -> parent.addLink.partially1(link)
                        else -> {
                            val either = link.getLinkedIssue()
                            either.fold(
                                { { it.left() } },
                                { issue ->
                                    issue.addLink.partially1(link.withIssue(parentKey, parent))
                                }
                            )
                        }
                    }
                }
            )
        }

    private fun toLinkAdders(parent: Pair<TransferVersionsModule.LinkedIssue, IssueDetails>, links: List<TransferVersionsModule.Link>) =
        links
            .filter(::hasLink.partially1(parent.second))
            .map(::toLinkAdder.partially2(parent))



    private fun hasLink(issue: IssueDetails, other: TransferVersionsModule.Link) =
        issue.links.any {
            it.type == other.type &&
                    it.outwards == other.outwards &&
                    it.issue.key == other.issue.key
        }

    private fun toLinkAdder(link: TransferVersionsModule.Link, parent: Pair<TransferVersionsModule.LinkedIssue, IssueDetails>) =
        when {
            link.outwards -> link.issue.addLink.partially2(parent.first.key)
            else -> parent.first.addLink.partially2(link.issue.key)
        }.partially1(link.type)

    private fun equal(l1: Link, l2: Link) =
        l1.inwards == l2.inwards &&
        l1.type == l2.type &&
        l1.linkedKey == l2.linkedKey

    private fun Link.withIssue(key: String, issue: LinkedIssue) =
        Link(type, inwards, key, { issue.right() }, { IllegalStateException().left() })
}