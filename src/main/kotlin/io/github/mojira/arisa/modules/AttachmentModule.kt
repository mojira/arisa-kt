package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import net.rcarz.jiraclient.Attachment

data class AttachmentModuleRequest(val attachments: List<Attachment>)

class AttachmentModule(
    val deleteAttachment: (Attachment) -> Either<Throwable, Unit>,
    val extensionBlackList: List<String>
) : Module<AttachmentModuleRequest> {

    override fun invoke(request: AttachmentModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val endsWithBlacklistedExtensionAdapter = ::endsWithBlacklistedExtensions.partially1(extensionBlackList)
        val blackListedAttachments = request.attachments.filter(endsWithBlacklistedExtensionAdapter)
        assertNotEmpty(blackListedAttachments).bind()
        tryRunAll(deleteAttachment, blackListedAttachments).bind()
    }
}

private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, attachment: Attachment) =
    extensionBlackList.any { attachment.contentUrl.endsWith(it) }
