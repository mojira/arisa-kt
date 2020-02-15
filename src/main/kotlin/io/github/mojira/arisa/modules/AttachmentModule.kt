package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
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
        deleteAttachmentsAdapter(deleteAttachment, blackListedAttachments).bind()
    }
}

private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, attachment: Attachment) =
    extensionBlackList.any { attachment.contentUrl.endsWith(it) }

private fun deleteAttachmentsAdapter(
    deleteAttachment: (Attachment) -> Either<Throwable, Unit>,
    blackListedAttachments: List<Attachment>
): Either<FailedModuleResponse, ModuleResponse> {
    val exceptions = blackListedAttachments
        .map(deleteAttachment)
        .filter { it.isLeft() }
        .map { (it as Either.Left).a }

    return if (exceptions.isEmpty()) {
        ModuleResponse.right()
    } else {
        FailedModuleResponse(exceptions).left()
    }
}

private fun assertNotEmpty(blackListedAttachments: List<Attachment>) = if (blackListedAttachments.isEmpty()) {
    OperationNotNeededModuleResponse.left()
} else {
    Unit.right()
}
