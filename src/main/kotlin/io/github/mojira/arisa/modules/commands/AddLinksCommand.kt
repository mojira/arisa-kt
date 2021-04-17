package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.addLinks
import io.github.mojira.arisa.modules.commands.arguments.LinkList

class AddLinksCommand {
    operator fun invoke(issue: Issue, linkList: LinkList): Int {
        val either = addLinks(issue, linkList.type, linkList.keys)
        return either.fold(
            { throw CommandExceptions.LEFT_EITHER.create(it) },
            { linkList.keys.size }
        )
    }

}
