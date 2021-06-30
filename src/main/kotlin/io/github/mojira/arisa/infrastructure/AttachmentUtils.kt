package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import com.urielsalis.mccrashlib.Crash
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.Issue
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

    data class CrashAttachment(
        val document: TextDocument,
        val crash: Crash
    )

    fun extractCrashesFromAttachments(issue: Issue): List<CrashAttachment> = with(issue) {
        // Get crashes from issue attachments
        val textDocuments = attachments
            // Ignore attachments from Arisa (e.g. deobfuscated crash reports)
            .filterNot { it.uploader?.isBotUser?.invoke() == true }

            // Only check attachments with allowed extensions
            .filter { isCrashAttachment(it.name) }

            // Download attachment
            .map(::fetchAttachment)
            .toMutableList()

        // Also add description, so it's searched for crash reports
        textDocuments.add(TextDocument({ description ?: "" }, created, "description"))

        // Analyze crash reports
        textDocuments
            .asSequence()
            .mapNotNull(::processCrash)
            .filter { it.crash is Crash.Minecraft || it.crash is Crash.Java }
            .toList()
    }

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val getText = attachment::getTextContent

        return TextDocument(getText, attachment.created, attachment.name)
    }

    // Processes the crash report in the text document.
    // Returns null if it cannot be processed, otherwise data about the crash.
    private fun processCrash(textDocument: TextDocument): CrashAttachment? {
        val processedCrash = crashReader.processCrash(textDocument.getContent().lines(), mappingsDir)

        if (processedCrash.isLeft()) return null

        return CrashAttachment(textDocument, (processedCrash as Either.Right<Crash>).b)
    }
}
