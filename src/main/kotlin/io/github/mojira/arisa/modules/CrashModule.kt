package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import java.util.Calendar
import java.util.Date
import kotlin.text.RegexOption.IGNORE_CASE

const val MINECRAFT_CRASH_HEADER = "---- Minecraft Crash Report ----"
const val JAVA_CRASH_HEADER = "#  EXCEPTION_ACCESS_VIOLATION"

class CrashModule(
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val maxAttachmentAge: Int
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

            val infos = textDocuments
                .filter(::isTextDocumentRecent)
                .mapNotNull(::fetchInfo)
            assertNotEmpty(infos).bind()

            val mostRelevantInfo = infos.reduce(::getMoreRelevantInfo)

            if (mostRelevantInfo.modded) {
                addModdedComment().toFailedModuleEither().bind()
                resolveAsInvalid().toFailedModuleEither().bind()
            } else {
                val configs = crashDupeConfigs.filter(::isConfigValid)
                val key = getDuplicateKey(mostRelevantInfo, configs)
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

    private fun isConfigValid(config: CrashDupeConfig) =
        CrashInfoType.values().any { it.name == config.type.toUpperCase() }

    private fun getMoreRelevantInfo(info1: CrashInfo, info2: CrashInfo) = when {
        !info1.exception.isNullOrBlank() && info2.exception.isNullOrBlank() -> info1
        info1.exception.isNullOrBlank() && !info2.exception.isNullOrBlank() -> info2
        info1.modded && !info2.modded -> info2
        !info1.modded && info2.modded -> info1
        info2.created.after(info1.created) -> info2
        else -> info1
    }

    private fun getDuplicateKey(info: CrashInfo, configs: List<CrashDupeConfig>) =
        configs.firstOrNull {
            CrashInfoType.valueOf(it.type.toUpperCase()) == info.type &&
                    info.exception != null &&
                    it.exceptionRegex.toRegex(IGNORE_CASE).containsMatchIn(info.exception)
        }?.duplicates

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val data = attachment.content
        val text = String(data)

        return TextDocument(text, attachment.created)
    }

    private fun fetchInfo(file: TextDocument) = when {
        file.content.contains(MINECRAFT_CRASH_HEADER, true) -> {
            val lines = file.content.split("\n")
            val exception = lines.getOrNull(6)
            var modded = false

            lines.forEach {
                if (it.contains("Is Modded", true)) {
                    modded = !it.contains("Probably Not", true) && !it.contains("Unknown", true)
                }
            }

            if (lines.size >= 7)
                CrashInfo(
                    CrashInfoType.MINECRAFT,
                    exception?.trim(),
                    modded,
                    file.created
                )
            else null
        }
        file.content.contains(JAVA_CRASH_HEADER, true) -> {
            val lines = file.content.split("\n")
            var error: String? = null

            lines.forEach {
                if (it.contains("# C  ", true)) {
                    error = it.substring(it.indexOf('[') + 1, it.indexOf('+'))
                }
            }

            if (error == null) {
                null
            } else {
                CrashInfo(CrashInfoType.JAVA, error!!.trim(), false, file.created)
            }
        }
        else -> null
    }

    data class TextDocument(
        val content: String,
        val created: Date
    )

    data class CrashInfo(
        val type: CrashInfoType,
        val exception: String?,
        val modded: Boolean,
        val created: Date
    )

    enum class CrashInfoType {
        MINECRAFT, JAVA
    }
}
