package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class ReopenCommand {
    operator fun invoke(issue: Issue): Int {
        if (issue.resolution != "Awaiting Response") {
            throw CommandExceptions.NOT_AR.create()
        }
        issue.reopen()
        return 1
    }
}
