package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.addLinks
import io.github.mojira.arisa.modules.commands.arguments.LinkList

class AddLinksCommand : Command1<LinkList> {
    override operator fun invoke(issue: Issue, arg: LinkList): Int {
        val either = addLinks(issue, arg.type, arg.keys)
        return either.fold(
            { throw CommandExceptions.LEFT_EITHER.create(it) },
            { arg.keys.size }
        )
    }
}
