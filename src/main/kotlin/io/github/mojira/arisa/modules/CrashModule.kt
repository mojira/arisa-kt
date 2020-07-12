package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.Some
import arrow.core.extensions.fx
import arrow.core.firstOrNone
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import me.urielsalis.mccrashlib.parser.ParserError
import java.time.Instant

class CrashModule(
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val crashReader: CrashReader,
    private val dupeMessage: String,
    private val moddedMessage: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertNull(priority).bind()

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

            assertContainsNewCrash(crashes, lastRun).bind()
            assertNoValidCrash(crashes).bind()

            val key = crashes
                .sortedByDescending { it.first.created }
                .mapNotNull(::getDuplicateLink.partially2(crashDupeConfigs))
                .firstOrNull()

            if (key == null) {
                resolveAsInvalid()
                addComment(CommentOptions(moddedMessage))
            } else {
                resolveAsDuplicate()
                addComment(CommentOptions(dupeMessage, key))
                createLink("Duplicate", key)
            }
        }
    }

    private fun extractCrash(it: Pair<TextDocument, Either<ParserError, Crash>>) =
        it.first to (it.second as Either.Right<Crash>).b

    private fun processCrash(it: TextDocument) = it to crashReader.processCrash(it.getContent().lines())

    @Suppress("ReturnCount")
    private fun getDuplicateLink(
        crash: Pair<TextDocument, Crash>,
        crashDupeConfigs: List<CrashDupeConfig>
    ): String? = with (crash.second) {
        val minecraftConfigs = crashDupeConfigs.filter { it.type == "minecraft" }
        val javaConfigs = crashDupeConfigs.filter { it.type == "java" }

            when (this) {
                is Crash.Minecraft -> {
                    val config =
                        minecraftConfigs.firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(exception) }
                    if (config.isDefined()) {
                        return (config as Some).t.duplicates
                    }
                }
                is Crash.Java -> {
                    val config = javaConfigs.firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(code) }
                    if (config.isDefined()) {
                        return (config as Some).t.duplicates
                    }
                }
            }
        return null
    }

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun isModded(crash: Pair<TextDocument, Crash>) =
        crash.second is Crash.Minecraft && (crash.second as Crash.Minecraft).modded

    private fun crashNewlyAdded(crash: Pair<TextDocument, Crash>, lastRun: Instant) =
        crash.first.created.isAfter(lastRun)

    private fun assertContainsNewCrash(crashes: List<Pair<TextDocument, Crash>>, lastRun: Instant) =
        if (crashes.any(::crashNewlyAdded.partially2(lastRun)))
            Unit.right()
        else
            OperationNotNeededModuleResponse.left()

    private fun assertNoValidCrash(crashes: List<Pair<TextDocument, Crash>>) =
        if (crashes.all { isModded(it) || getDuplicateLink(it, crashDupeConfigs) != null })
            Unit.right()
        else
            OperationNotNeededModuleResponse.left()

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
