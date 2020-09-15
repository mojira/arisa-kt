package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertTrue
import io.github.mojira.arisa.modules.toFailedModuleEither
import kotlinx.coroutines.runBlocking

class PurgeAttachmentCommand : Command {
    @Suppress("MagicNumber")
    override fun invoke(commandInfo: CommandInfo, issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size <= 3).bind()
        val startID = arguments.getOrNull(1)?.toIntEither()?.bind() ?: 0
        val endID = arguments.getOrNull(2)?.toIntEither()?.bind() ?: Int.MAX_VALUE
        for (attachment in issue.attachments) {
            val attachmentID = attachment.id.toIntEither().bind()
            if (attachmentID in startID..endID) {
                attachment.remove()
            }
        }
    }

    private fun String.toIntEither() = runBlocking {
        Either.catch {
            toInt()
        }
    }.toFailedModuleEither()
}
