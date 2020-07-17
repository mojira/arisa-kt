package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import me.urielsalis.mccrashlib.parser.ParserError
import java.time.Instant

class MissingCrashModule(
    private val crashReportExtensions: List<String>,
    private val crashReader: CrashReader,
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertEquals(status, "Open").bind()
            assertNull(priority).bind()
            assertContains(description, "crash").bind()

            val textDocuments = attachments
                .filter { isCrashAttachment(it.name) }
                .map(::fetchAttachment)
                .toMutableList()
            // also add description, so it's searched for crash reports
            textDocuments.add(TextDocument({ description ?: "" }, created))

            val crashes = textDocuments
                .asSequence()
                .map { processCrash(it) }
                .filter { it.second.isRight() }
                .map { extractCrash(it) }
                .filter { it.second is Crash.Minecraft || it.second is Crash.Java }
                .toList()

            assertEmpty(crashes).bind()

            resolveAsAwaitingResponse()
            addComment(CommentOptions(message))
        }
    }

    private fun extractCrash(it: Pair<TextDocument, Either<ParserError, Crash>>) =
        it.first to (it.second as Either.Right<Crash>).b

    private fun processCrash(it: TextDocument) = it to crashReader.processCrash(it.getContent().lines())

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val getText = {
            val data = attachment.getContent()
            String(data)
        }

        return TextDocument(getText, attachment.created)
    }

    data class TextDocument(
        val getContent: () -> String,
        val created: Instant
    )
}
