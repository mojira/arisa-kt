package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.modules.deleteLinks

class DeleteLinksCommand {
    operator fun invoke(issue: Issue, linkList: LinkList): Int {
        val either = deleteLinks(issue, linkList.type, linkList.keys)
        return either.fold(
            { throw CommandExceptions.LEFT_EITHER.create(it) },
            { linkList.keys.size }
        )
    }
}
