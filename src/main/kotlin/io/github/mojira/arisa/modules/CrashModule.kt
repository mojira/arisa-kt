package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.Some
import arrow.core.extensions.fx
import arrow.core.firstOrNone
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import me.urielsalis.mccrashlib.parser.ParserError
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.SortedMap

class CrashModule(
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val maxAttachmentAge: Int,
    private val crashReader: CrashReader
) : Module<CrashModule.Request> {

    data class Request(
        val attachments: List<Attachment>,
        val body: String?,
        val created: Instant,
        val confirmationStatus: String?,
        val priority: String?,
        val resolveAsInvalid: () -> Either<Throwable, Unit>,
        val resolveAsDuplicate: () -> Either<Throwable, Unit>,
        val linkDuplicate: (key: String) -> Either<Throwable, Unit>,
        val addModdedComment: () -> Either<Throwable, Unit>,
        val addDuplicateComment: (key: String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertNull(priority).bind()

            val textDocuments = attachments
                .filter { isCrashAttachment(it.name) }
                .map(::fetchAttachment)
                .toMutableList()
            textDocuments.add(TextDocument({ body ?: "" }, created))

            val crashes = textDocuments
                .asSequence()
                .filter(::isTextDocumentRecent)
                .map { processCrash(it) }
                .filter { it.second.isRight() }
                .map { extractCrash(it) }
                .filter { it.second is Crash.Minecraft || it.second is Crash.Java }
                .toList()

            assertNotEmpty(crashes).bind()

            val anyModded = crashes.any { it.second is Crash.Minecraft && (it.second as Crash.Minecraft).modded }
            val sortedMap = crashes.toMap().toSortedMap(compareByDescending { it.created })
            val key = findDuplicate(sortedMap, crashDupeConfigs)

            if (key == null) {
                if (anyModded) {
                    addModdedComment().toFailedModuleEither().bind()
                    resolveAsInvalid().toFailedModuleEither().bind()
                } else {
                    assertNotNull(key).bind()
                }
            } else {
                addDuplicateComment(key).toFailedModuleEither().bind()
                resolveAsDuplicate().toFailedModuleEither().bind()
                linkDuplicate(key).toFailedModuleEither().bind()
            }
        }
    }

    private fun extractCrash(it: Pair<TextDocument, Either<ParserError, Crash>>) =
        it.first to (it.second as Either.Right<Crash>).b

    private fun processCrash(it: TextDocument) = it to crashReader.processCrash(it.getContent().lines())

    private fun findDuplicate(
        sortedMap: SortedMap<TextDocument, Crash>,
        crashDupeConfigs: List<CrashDupeConfig>
    ): String? {
        val minecraftConfigs = crashDupeConfigs.filter { it.type == "minecraft" }
        val javaConfigs = crashDupeConfigs.filter { it.type == "java" }

        sortedMap.forEach { (_, crash) ->
            when (crash) {
                is Crash.Minecraft -> {
                    val config =
                        minecraftConfigs.firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(crash.exception) }
                    if (config.isDefined()) {
                        return (config as Some).t.duplicates
                    }
                }
                is Crash.Java -> {
                    val config = javaConfigs.firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(crash.code) }
                    if (config.isDefined()) {
                        return (config as Some).t.duplicates
                    }
                }
            }
        }
        return null
    }

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun isTextDocumentRecent(textDocument: TextDocument): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -maxAttachmentAge)

        return Instant.now()
            .isAfter(
                textDocument.created
                    .plus(maxAttachmentAge.toLong(), ChronoUnit.DAYS)
            )
    }

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
