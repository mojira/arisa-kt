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
        val acceptedTicketKeys = ticketKeys.map {
            it.toUpperCase()
        }.filter {
            if (it == issue.key)
                false
            else {
                val ticketProject = it.takeWhile { char ->
                    char in 'A'..'Z'
                }
                ticketProject == issue.project.key || (issue.project.key in allowedCrossProjectDuplicates.keys
                        && ticketProject in allowedCrossProjectDuplicates.getValue(issue.project.key))
            }
        }
        assertNotEmpty(acceptedTicketKeys)
        acceptedTicketKeys.mapNotNull {
            val childIssueEither = issue.getOtherIssue(it)
            val childIssue = if (childIssueEither.isRight()) (childIssueEither as Either.Right).b else null
            if (childIssue?.resolution.getOrDefault("Unresolved") == "Unresolved") childIssue else null
        }.forEach {
            it.resolveAsDuplicate()
            it.createLink("Duplicate", issue.key, true)
        }
    }

    private val allowedCrossProjectDuplicates = mapOf(
        "REALMS" to listOf("MC", "MCPE"),
        "MC" to listOf("MCL", "REALMS"),
        "MCPE" to listOf("BDS", "REALMS"),
        "MCL" to listOf("MC"),
        "BDS" to listOf("MCPE")
    )

    private fun String?.getOrDefault(default: String) =
            if (isNullOrBlank())
                default
            else
                this
}
