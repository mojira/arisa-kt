package io.github.mojira.arisa.application


import arrow.core.Either
import io.github.mojira.arisa.domain.model.Attachment
import io.github.mojira.arisa.domain.service.DeleteAttachmentService

val invalidExtensions = listOf("jar", "exe", "com", "bat", "msi", "run", "lnk", "dmg")

class AttachmentModule(
    val deleteAttachmentService: DeleteAttachmentService
) : Module<AttachmentModuleRequest> {
    override fun invoke(request: AttachmentModuleRequest): ModuleResponse {
        val failedRequests = request
            .attachments
            .filter(::endsWithBlacklistedExtensions)
            .map { deleteAttachmentService.deleteAttachment(it) }
            .filterIsInstance<Either.Left<Exception>>()
            .map { it.a }

        return if (failedRequests.isNotEmpty()) {
            FailedModuleResponse(failedRequests)
        } else {
            SucessfulModuleResponse
        }
    }

    private fun endsWithBlacklistedExtensions(attachment: Attachment): Boolean =
        invalidExtensions.any { attachment.contentUrl.endsWith(it) }
}

data class AttachmentModuleRequest(val attachments: List<Attachment>)