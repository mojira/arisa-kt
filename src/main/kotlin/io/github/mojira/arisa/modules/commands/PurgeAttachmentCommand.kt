package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class PurgeAttachmentCommand : Command2<Int, Int> {
    override operator fun invoke(issue: Issue, arg1: Int, arg2: Int): Int {
        var result = 0
        for (attachment in issue.attachments) {
            val attachmentID = attachment.id.toInt()
            if (attachmentID in arg1..arg2) {
                attachment.remove()
                result++
            }
        }
        return result
    }
}
