package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse

class PurgeAttachmentCommand {
    @Suppress("MagicNumber")
    fun invoke(issue: Issue, start: Int, end: Int = Int.MAX_VALUE): Int {
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
