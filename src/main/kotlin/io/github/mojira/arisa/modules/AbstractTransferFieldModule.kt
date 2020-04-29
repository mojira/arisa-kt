package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Request

abstract class AbstractTransferFieldModule<FIELD> : Module<Request<FIELD>> {
    data class LinkedIssue<FIELD>(
        val key: String,
        val status: String,
        val transferFieldTo: (key: String) -> Either<Throwable, Unit>,
        val getField: () -> Either<Throwable, FIELD>
    )

    data class Link<FIELD>(
        val type: String,
        val outwards: Boolean,
        val issue: LinkedIssue<FIELD>
    )

    data class Request<FIELD>(
        val key: String,
        val links: List<Link<FIELD>>,
        val field: FIELD
    )

    override fun invoke(request: Request<FIELD>): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val relevantParents = links
                .filter(::isDuplicatesLink)
                .map { it.issue }
                .filter(::filterParents.partially2(request))
            assertGreaterThan(relevantParents.size, 0).bind()

            val parentEithers = relevantParents
                .map(::toIssueVersionPair)

            parentEithers.toFailedModuleEither().bind()
            val parents = parentEithers
                .map { (it as Either.Right).b }

            val functions = parents
                .flatMap(::toFunctions.partially2(field))

            assertNotEmpty(functions).bind()
            tryRunAll(functions).bind()
        }
    }

    private fun toIssueVersionPair(issue: LinkedIssue<FIELD>): Either<Throwable, Pair<LinkedIssue<FIELD>, FIELD>> {
        val details = issue.getField()
        return details.fold(
            { it.left() },
            { (issue to it).right() }
        )
    }

    protected open fun filterParents(issue: LinkedIssue<FIELD>, request: Request<FIELD>) =
        true


    protected abstract fun toFunctions(parent: Pair<LinkedIssue<FIELD>, FIELD>, field: FIELD): Collection<() -> Either<Throwable, Unit>>

    private fun isDuplicatesLink(link: Link<*>) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}