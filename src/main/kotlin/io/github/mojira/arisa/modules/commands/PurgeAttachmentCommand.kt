package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class PurgeAttachmentCommand {
    operator fun invoke(issue: Issue, userName: String, minId: Int, maxId: Int): Int {
        return issue.attachments
            .filter {
                // Make sure that attachment is uploaded by the specified user
                it.uploader?.name == userName
            }
            .filter {
                // Don't delete attachments with an ID outside of ID range
                it.id.toInt() in minId..maxId
            }
            .onEach { it.remove() }
            .count()
    }
}
