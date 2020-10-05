package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.*

class DuplicatedByCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 1).bind()
        val ticketKeys = arguments.toMutableList().subList(1, arguments.size).apply {
            this.splitElemsByCommas()
            this.convertLinks()
        }
        assertTrue(ticketKeys.all { it.isTicketKey() }).bind()
        ticketKeys.mapNotNull {
            val childIssueEither = issue.getOtherIssue(it)
            val childIssue = if (childIssueEither.isRight()) (childIssueEither as Either.Right).b else null
            if (childIssue?.resolution == "Unresolved") childIssue else null
        }.forEach {
            it.resolveAsDuplicate()
            it.createLink("Duplicate", issue.key, true)
        }
    }
}