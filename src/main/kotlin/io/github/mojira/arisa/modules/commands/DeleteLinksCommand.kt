package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.*

class DeleteLinksCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 2).bind()
        var args = listOf(*arguments).subList(1, arguments.size).toTypedArray()
        args = splitArrayByCommas(*args)
        args = concatLinkName(*args)
        val type = args[0]
        assertFalse(type == "").bind()
        args = listOf(*args).subList(1, args.size).toTypedArray()
        args = convertLinks(*args)
        deleteLinks(issue, type, *args).bind()
    }
}
