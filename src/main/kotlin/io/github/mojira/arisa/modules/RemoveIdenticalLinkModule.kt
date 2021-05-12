package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import java.time.Instant

class RemoveIdenticalLinkModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotEmpty(links).bind()

            val removeLinkFunctions = links
                .filter(::isRelatesLink)
                .groupBy { it.issue.key }
                .filterValues { it.size == 2 }
                .map {
                    // Always remove the outwards link belonging to the smaller ticket key,
                    // so that when this module is triggered on both tickets, only one link is removed.
                    it.value.find { l -> if (key < it.key) l.outwards else !l.outwards }!!.remove
                }

            assertNotEmpty(removeLinkFunctions).bind()

            removeLinkFunctions.forEach { it.invoke() }
        }
    }

    private fun isRelatesLink(link: Link): Boolean = link.type.toLowerCase() == "relates"
}
