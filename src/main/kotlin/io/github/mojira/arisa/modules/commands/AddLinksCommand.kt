package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.*

class AddLinksCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 2).bind()
        val list = mutableListOf(*arguments).subList(1, arguments.size).apply {
            this.splitElemsByCommas()
            this.concatLinkName()
        }
        val type = list[0]
        assertFalse(type == "").bind()
        list.apply {
            this.removeAt(0)
            this.convertLinks()
        }
        assertTrue(list.all{ it.matches(Regex("[A-Z]+-[0-9]+")) }).bind()
        val args = list.toTypedArray()
        addLinks(issue, type, *args).bind()
    }
}
