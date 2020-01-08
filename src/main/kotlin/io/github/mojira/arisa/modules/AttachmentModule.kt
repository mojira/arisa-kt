package io.github.mojira.arisa.modules

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.Arisa
import net.rcarz.jiraclient.Attachment
import net.rcarz.jiraclient.JiraClient

class AttachmentModule(jiraClient: JiraClient, config: Config) : Module<AttachmentModuleRequest>(jiraClient, config) {
    override fun invoke(request: AttachmentModuleRequest): ModuleResponse {
        val failedRequests = request
            .attachments
            .filter(::endsWithBlacklistedExtensions)
            .map { deleteAttachment(it) }
            .filterIsInstance<Either.Left<Exception>>()
            .map { it.a }

        return when {
            request.attachments.isEmpty() -> OperationNotNeededModuleResponse
            failedRequests.isNotEmpty() -> FailedModuleResponse(failedRequests)
            else -> SucessfulModuleResponse
        }
    }

    private fun deleteAttachment(attachment: Attachment) = try {
        jiraClient.restClient.delete(Attachment.getBaseUri() + attachment.id)
        Either.right(Unit)
    } catch (e: java.lang.Exception) {
        Either.left(e)
    }

    private fun endsWithBlacklistedExtensions(attachment: Attachment) =
        config[Arisa.Modules.Attachment.extensionBlacklist]
            .split(",")
            .any { attachment.contentUrl.endsWith(it) }
}

data class AttachmentModuleRequest(val attachments: List<Attachment>)