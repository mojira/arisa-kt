package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.modules.deleteLinks

class DeleteLinksCommand : Command1<LinkList> {
    override operator fun invoke(issue: Issue, arg: LinkList): Int {
        val either = deleteLinks(issue, arg.type, arg.keys)
        return either.fold(
            { throw CommandExceptions.LEFT_EITHER.create(it) },
            { arg.keys.size } // TODO: Returns the actual amount of links deleted.
        )
    }
}
