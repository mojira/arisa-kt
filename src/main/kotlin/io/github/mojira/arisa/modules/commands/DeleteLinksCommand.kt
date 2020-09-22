package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.*

class DeleteLinksCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 2).bind()
        val list = mutableListOf(*arguments).subList(1, arguments.size)
        splitElemsByCommas(list)
        concatLinkName(list)
        val type = list[0]
        assertFalse(type == "").bind()
        list.removeAt(0)
        convertLinks(list)
        val args = list.toTypedArray()
        deleteLinks(issue, type, *args).bind()
    }
}
