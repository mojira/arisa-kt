package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import java.time.Instant

abstract class AbstractTransferFieldModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val relevantParents = links
                .filter(::isDuplicatesLink)
                .map { it.issue }
                .filter(::filterParents.partially2(issue))
            assertGreaterThan(relevantParents.size, 0).bind()

            val parentEithers = relevantParents
                .map(::toFullIssue)

            parentEithers.toFailedModuleEither().bind()
            val parents = parentEithers
                .map { (it as Either.Right).b }

            val functions = getFunctions(parents, issue)

            assertNotEmpty(functions).bind()
            functions.forEach { it.invoke() }
        }
    }

    private fun toFullIssue(
        issue: LinkedIssue
    ): Either<Throwable, Issue> {
        val fullIssue = issue.getFullIssue()
        return fullIssue.fold(
            { it.left() },
            { it.right() }
        )
    }

    protected open fun filterParents(linkedIssue: LinkedIssue, issue: Issue) =
        true

    protected abstract fun getFunctions(
        parents: Collection<Issue>,
        issue: Issue
    ): Collection<() -> Unit>

    protected fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}
