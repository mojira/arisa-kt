package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertTrue

class DeleteCommentsCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 1).bind()
        val name = arguments.asList().subList(1, arguments.size).joinToString(" ")
        val comments = issue.comments
        comments.filter { it.visibilityValue != "staff" }.filter { it.author.name == name }.forEach {
            it.restrict("Removed by arisa")
        }
        ModuleResponse.right()
    }
}
