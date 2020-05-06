package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Request

abstract class AbstractTransferFieldModule<FIELD, FUNPARAM> : Module<Request<FIELD, FUNPARAM>> {
    data class Request<FIELD, FUNPARAM>(
        val key: String,
        val links: List<Link<FIELD, FUNPARAM>>,
        val field: FIELD
    )

    override fun invoke(request: Request<FIELD, FUNPARAM>): Either<ModuleError, ModuleResponse> = with(request) {
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

            val functions = getFunctions(parents, field)

            assertNotEmpty(functions).bind()
            tryRunAll(functions).bind()
        }
    }

    private fun toIssueVersionPair(issue: LinkedIssue<FIELD, FUNPARAM>): Either<Throwable, Pair<LinkedIssue<FIELD, FUNPARAM>, FIELD>> {
        val details = issue.getField()
        return details.fold(
            { it.left() },
            { (issue to it).right() }
        )
    }

    protected open fun filterParents(issue: LinkedIssue<FIELD, *>, request: Request<FIELD, *>) =
        true

    protected abstract fun getFunctions(parents: Collection<Pair<LinkedIssue<FIELD, FUNPARAM>, FIELD>>, field: FIELD): Collection<() -> Either<Throwable, Unit>>

    protected fun isDuplicatesLink(link: Link<*, *>) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}
