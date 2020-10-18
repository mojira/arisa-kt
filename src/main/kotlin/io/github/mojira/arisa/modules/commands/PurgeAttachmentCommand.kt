package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class PurgeAttachmentCommand {
    @Suppress("MagicNumber")
    operator fun invoke(issue: Issue, start: Int, end: Int = Int.MAX_VALUE): Int {
        var result = 0
        for (attachment in issue.attachments) {
            val attachmentID = attachment.id.toInt()
            if (attachmentID in start..end) {
                attachment.remove()
                result++
            }
        }
        return result
    }
}
