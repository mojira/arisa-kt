package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.Issue
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import me.urielsalis.mccrashlib.parser.ParserError
import java.io.File
import java.time.Instant

class AttachmentUtils(
    private val crashReportExtensions: List<String>,
    private val crashReader: CrashReader
) {
    private val mappingsDir by lazy {
        val file = File("mc-mappings")
        if (!file.exists()) file.mkdir()
        file
    }

    data class TextDocument(
        val getContent: () -> String,
        val created: Instant,
        val name: String
    )

    fun extractCrashesFromAttachments(issue: Issue): List<Pair<TextDocument, Crash>> = with(issue) {
        val textDocuments = attachments
            .filter { isCrashAttachment(it.name) }
            .map(::fetchAttachment)
            .toMutableList()
        // also add description, so it's searched for crash reports
        textDocuments.add(TextDocument({ description ?: "" }, created, "description"))

        textDocuments
            .asSequence()
            .map { processCrash(it) }
            .filter { it.second.isRight() }
            .map { extractCrash(it) }
            .filter { it.second is Crash.Minecraft || it.second is Crash.Java }
            .toList()
    }

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val getText = {
            val data = attachment.getContent()
            String(data)
        }

        return TextDocument(getText, attachment.created, attachment.name)
    }

    private fun processCrash(it: TextDocument) = it to crashReader.processCrash(it.getContent().lines(), mappingsDir)

    private fun extractCrash(it: Pair<TextDocument, Either<ParserError, Crash>>) =
        it.first to (it.second as Either.Right<Crash>).b
}
