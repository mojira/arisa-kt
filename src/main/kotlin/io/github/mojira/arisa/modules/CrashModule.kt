package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import net.rcarz.jiraclient.Attachment
import java.util.*
import kotlin.text.RegexOption.IGNORE_CASE

data class CrashModuleRequest(
    val attachments: List<Attachment>,
    val body: String,
    val created: Date
)


const val MINECRAFT_CRASH_HEADER = "---- Minecraft Crash Report ----"
const val JAVA_CRASH_HEADER = "#  EXCEPTION_ACCESS_VIOLATION"

class CrashModule(
    private val resolveAsInvalid: () -> Either<Throwable, Unit>,
    private val resolveAsDuplicate: () -> Either<Throwable, Unit>,
    private val linkDuplicate: (key: String) -> Either<Throwable, Unit>,
    private val addModdedComment: () -> Either<Throwable, Unit>,
    private val addDuplicateComment: (key: String) -> Either<Throwable, Unit>,
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val maxAttachmentAge: Int
) : Module<CrashModuleRequest> {
    override fun invoke(request: CrashModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val crashAttachments = request.attachments.filter(::isCrashAttachment)

        val textDocuments = crashAttachments
            .map(::fetchAttachment)
            .toMutableList()
        textDocuments.add(TextDocument(request.body, request.created))

        val recentTextDocuments = textDocuments.filter(::isTextDocumentRecent)
        val infos = recentTextDocuments.mapNotNull(::fetchInfo)
        assertNotEmpty(infos).bind()

        val mostRelevantInfo = infos.reduce(::getMoreRelevantInfo)

        if (mostRelevantInfo.modded) {
            addModdedComment().toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
        else {
            val configs = crashDupeConfigs.filter(::isConfigValid)
            val key = getDuplicateKey(mostRelevantInfo, configs)
            assertNotNull(key).bind()

            addDuplicateComment(key!!).toFailedModuleEither().bind()
            resolveAsDuplicate().toFailedModuleEither().bind()
            linkDuplicate(key).toFailedModuleEither().bind()
        }

    }

    private fun isCrashAttachment(attachment: Attachment) =
        crashReportExtensions.any { it == attachment.mimeType }

    private fun isTextDocumentRecent(textDocument: TextDocument): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -maxAttachmentAge)

        return textDocument.created.after(calendar.time)
    }

    private fun isConfigValid(config: CrashDupeConfig) =
        CrashInfoType.values().any { it.name == config.type.toUpperCase() }

    private fun getMoreRelevantInfo(info1: CrashInfo, info2: CrashInfo) = when {
        info1.exception.isNotBlank() && info2.exception.isBlank() -> info1
        info1.exception.isBlank() && info2.exception.isNotBlank() -> info2
        info1.modded && !info2.modded -> info2
        !info1.modded && info2.modded -> info1
        info2.created.after(info1.created) -> info2
        else -> info1
    }

    private fun getDuplicateKey(info: CrashInfo, configs: List<CrashDupeConfig>) =
        configs.firstOrNull{
            CrashInfoType.valueOf(it.type.toUpperCase()) == info.type
                    && it.exceptionDesc.toRegex(IGNORE_CASE).containsMatchIn(info.exception)
        }?.duplicates

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val data = attachment.download()
        val text = String(data)

        return TextDocument(text, attachment.createdDate)
    }

    private fun fetchInfo(file: TextDocument) = when {
        file.content.contains(MINECRAFT_CRASH_HEADER, true) -> {
            val lines = file.content.split("\n")
            val exception = lines[6]
            var minecraftVersion: String? = null
            var javaVersion: String? = null
            var modded = false

            lines.forEach {
                if(it.contains("Minecraft Version: ", true))
                    minecraftVersion = it.replace("Minecraft Version: ", "", true)

                if(it.contains("Java Version: ", true))
                    javaVersion = it.replace("Java Version: ", "", true)

                if(it.contains("Is Modded", true))
                    modded = !it.contains("Probably Not", true)
            }

            if(minecraftVersion != null && javaVersion != null && lines.size >= 7)
                CrashInfo(CrashInfoType.MINECRAFT, exception.trim(), minecraftVersion!!.trim(), javaVersion!!.trim(), modded, file.created)
            else null

        }
        file.content.contains(JAVA_CRASH_HEADER, true) -> {
            val lines = file.content.split("\n")
            var error: String? = null
            var javaVersion: String? = null

            lines.forEach {
                if(it.contains("# C  ", true))
                    error = it.substring(it.indexOf('[') + 1, it.indexOf('+'))

                if(it.contains("# JRE version: ", true))
                    javaVersion = it.replace("# JRE version: ", "", true)
            }

            if (error != null && javaVersion != null)
                CrashInfo(CrashInfoType.JAVA, error!!.trim(), null, javaVersion!!.trim(), false, file.created)
            else null
        }
        else -> null
    }


    data class TextDocument(
        val content: String,
        val created: Date
    )

    data class CrashInfo(
        val type: CrashInfoType,
        val exception: String,
        val minecraftVersion: String?,
        val javaVersion: String,
        val modded: Boolean,
        val created: Date
    )

    enum class CrashInfoType {
        MINECRAFT, JAVA
    }
}