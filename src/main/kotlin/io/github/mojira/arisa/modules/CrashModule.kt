package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.Some
import arrow.core.extensions.fx
import arrow.core.firstOrNone
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import java.util.Calendar
import java.util.Date

const val MINECRAFT_CRASH_HEADER = "---- Minecraft Crash Report ----"
const val JAVA_CRASH_HEADER = "#  EXCEPTION_ACCESS_VIOLATION"

class CrashModule(
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val maxAttachmentAge: Int,
    private val crashReader: CrashReader
) : Module<CrashModule.Request> {
    data class Attachment(
        val name: String,
        val created: Date,
        val content: ByteArray
    )

    data class Request(
        val attachments: List<Attachment>,
        val body: String?,
        val created: Date,
        val resolveAsInvalid: () -> Either<Throwable, Unit>,
        val resolveAsDuplicate: () -> Either<Throwable, Unit>,
        val linkDuplicate: (key: String) -> Either<Throwable, Unit>,
        val addModdedComment: () -> Either<Throwable, Unit>,
        val addDuplicateComment: (key: String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val textDocuments = attachments
                .filter { isCrashAttachment(it.name) }
                .map(::fetchAttachment)
                .toMutableList()
            textDocuments.add(TextDocument(body ?: "", created))

            val crashes = textDocuments
                .filter(::isTextDocumentRecent)
                .map { crashReader.processCrash(it.content.lines()) }
                .filterIsInstance<Either.Right<Crash>>()
                .map { it.b }

            assertNotEmpty(crashes).bind()

            val minecraftCrashes = crashes.filterIsInstance<Crash.Minecraft>()
            val javaCrashes = crashes.filterIsInstance<Crash.Java>()
            val minecraftConfigs = crashDupeConfigs.filter { it.type == "minecraft" }
            val javaConfigs = crashDupeConfigs.filter { it.type == "java" }

            if (minecraftCrashes.any(Crash.Minecraft::modded)) {
                addModdedComment().toFailedModuleEither().bind()
                resolveAsInvalid().toFailedModuleEither().bind()
            } else {
                val minecraftKeyMaybe = minecraftCrashes.map { crash ->
                    minecraftConfigs.firstOrNone { crash.exception.contains(it.exceptionRegex) }
                }.firstOrNone { !it.isEmpty() }.map { (it as Some).t }

                val javaKeyMaybe = javaCrashes.map { crash ->
                    javaConfigs.firstOrNone { crash.code.contains(it.exceptionRegex) }
                }.firstOrNone { !it.isEmpty() }.map { (it as Some).t }

                val key = if (minecraftKeyMaybe.isDefined()) {
                    (minecraftKeyMaybe as Some).t.duplicates
                } else if (javaKeyMaybe.isDefined()) {
                    (javaKeyMaybe as Some).t.duplicates
                } else {
                    null
                }

                assertNotNull(key).bind()
                addDuplicateComment(key!!).toFailedModuleEither().bind()
                resolveAsDuplicate().toFailedModuleEither().bind()
                linkDuplicate(key).toFailedModuleEither().bind()
            }
        }
    }

    private fun isCrashAttachment(fileName: String) =
        crashReportExtensions.any { it == fileName.substring(fileName.lastIndexOf(".") + 1) }

    private fun isTextDocumentRecent(textDocument: TextDocument): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -maxAttachmentAge)

        return textDocument.created.after(calendar.time)
    }

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val data = attachment.content
        val text = String(data)

        return TextDocument(text, attachment.created)
    }

    data class TextDocument(
        val content: String,
        val created: Date
    )
}
