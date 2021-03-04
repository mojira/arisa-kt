package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class PurgeAttachmentCommand : Command2<String, Int> {
    override operator fun invoke(issue: Issue, arg1: String, arg2: Int): Int {
        return issue.attachments
            .filter {
                // Make sure that attachment is uploaded by the specified user
                it.uploader?.name == arg1
            }
            .filter {
                // Don't delete attachments with an ID smaller than specified ID
                it.id.toInt() >= arg2
            }
            .onEach { it.remove() }
            .count()
    }
}
