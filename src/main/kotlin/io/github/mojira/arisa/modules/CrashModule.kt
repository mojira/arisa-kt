package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.Attachment
import java.util.*

data class CrashModuleRequest(
    val attachments: List<Attachment>,
    val body: String,
    val created: Date
)

class CrashModule(
    val crashExtensions: List<String>
) : Module<CrashModuleRequest> {
    override fun invoke(request: CrashModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val crashAttachments = request.attachments.filter(::isCrashAttachment)
        assertNotEmpty(crashAttachments).bind()

        val textDocuments = crashAttachments
            .map(::fetchAttachment)
            .toMutableList()
        textDocuments.add(TextDocument(request.body, request.created))
    }

    private fun isCrashAttachment(attachment: Attachment) =
        crashExtensions.any { it == attachment.mimeType }

    private fun fetchAttachment(attachment: Attachment): TextDocument {
        val data = attachment.download()
        val text = String(data)

        return TextDocument(text, attachment.createdDate)
    }

    data class TextDocument(
        val content: String,
        val created: Date
    )

    data class CrashInfo(
        val type: CrashInfoType,
        val exception: String,
        val minecraftVersion: String,
        val javaVersion: String,
        val modded: Boolean,
        val created: Date
    )

    enum class CrashInfoType {
        GAME, MINECRAFT
    }
}