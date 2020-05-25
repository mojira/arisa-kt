package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveIdenticalLinkModule : Module {
    data class GroupingLink(
        val type: String,
        val outwards: Boolean,
        val key: String
    )

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotEmpty(links).bind()

            val removeLinkFunctions = links
                .groupBy { GroupingLink(it.type, it.outwards, it.issue.key) }
                .filterValues { it.size > 1 }
                .values
                .flatMap { list ->
                    list.subList(1, list.size).map { it.remove }
                }

            assertNotEmpty(removeLinkFunctions).bind()

            removeLinkFunctions.forEach { it.invoke() }
        }
    }
}
