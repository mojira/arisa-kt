package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertTrue
import io.github.mojira.arisa.modules.assertFalse
import io.github.mojira.arisa.modules.concatLinkName
import io.github.mojira.arisa.modules.convertLinks
import io.github.mojira.arisa.modules.deleteLinks
import io.github.mojira.arisa.modules.isTicketKey
import io.github.mojira.arisa.modules.splitElemsByCommas

class DeleteLinksCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 2).bind()
        val list = arguments.toMutableList().subList(1, arguments.size).apply {
            this.splitElemsByCommas()
            this.concatLinkName()
        }
        val type = list[0]
        assertFalse(type == "").bind()
        list.apply {
            this.removeAt(0)
            this.convertLinks()
        }
        assertTrue(list.all { it.isTicketKey() }).bind()
        deleteLinks(issue, type, list).bind()
    }
}
