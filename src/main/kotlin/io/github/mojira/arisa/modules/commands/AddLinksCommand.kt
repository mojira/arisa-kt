package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.addLinks
import io.github.mojira.arisa.modules.assertTrue
import io.github.mojira.arisa.modules.assertFalse
import io.github.mojira.arisa.modules.concatLinkName
import io.github.mojira.arisa.modules.convertLinks

class AddLinksCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 2).bind()
        val tmpArray = listOf(*arguments).subList(1, arguments.size).toTypedArray()
        var args = concatLinkName(*tmpArray)
        val type = args[0]
        assertFalse(type == "").bind()
        args = listOf(*args).subList(1, args.size).toTypedArray()
        args = convertLinks(*args)
        addLinks(issue, type, *args).bind()
    }
}
