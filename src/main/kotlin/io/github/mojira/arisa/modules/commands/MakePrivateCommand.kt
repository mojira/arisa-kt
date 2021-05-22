package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class MakePrivateCommand {
    operator fun invoke(issue: Issue): Int {
        if (issue.securityLevel != null) {
            throw CommandExceptions.ALREADY_PRIVATE.create()
        }
        issue.setPrivate()
        return 1
    }
}
