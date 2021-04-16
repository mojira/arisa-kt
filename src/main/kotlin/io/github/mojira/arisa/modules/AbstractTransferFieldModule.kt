package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.toOption
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import java.time.Instant

abstract class AbstractTransferFieldModule : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val relevantParents = links
                .filter(::isDuplicatesLink)
                .filter(::createdSinceLastRun.partially2(changeLog).partially2(lastRun))
                .mapNotNull { it.issue }
                .filter(::filterParents.partially2(issue))
            assertGreaterThan(relevantParents.size, 0).bind()

            val parents = relevantParents
                .mapNotNull(::toFullIssue)

            val functions = getFunctions(parents, issue)

            assertNotEmpty(functions).bind()
            functions.forEach { it.invoke() }
        }
    }

    private fun toFullIssue(
        issue: LinkedIssue
    ): Issue? = issue.issue?.get()

    private fun createdSinceLastRun(link: Link, changeLog: List<ChangeLogItem>, lastRun: Instant) =
        changeLog.lastOrNull { it.changedTo == link.issue?.key }?.created?.isAfter(lastRun) ?: false

    protected open fun filterParents(linkedIssue: LinkedIssue, issue: Issue) =
        true

    protected abstract fun getFunctions(
        parents: Collection<Issue>,
        issue: Issue
    ): Collection<() -> Unit>

    protected fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}
