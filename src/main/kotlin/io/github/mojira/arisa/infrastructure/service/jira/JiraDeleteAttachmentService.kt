package io.github.mojira.arisa.infrastructure.service.jira

import arrow.core.Either
import io.github.mojira.arisa.domain.model.Attachment
import io.github.mojira.arisa.domain.service.DeleteAttachmentService
import net.rcarz.jiraclient.JiraClient
import java.lang.Exception

class JiraDeleteAttachmentService(
    val jiraClient: JiraClient
) : DeleteAttachmentService {
    override fun deleteAttachment(attachment: Attachment): Either<Exception, Unit> = try {
        jiraClient.restClient.delete(net.rcarz.jiraclient.Attachment.getBaseUri() + attachment.id)
        Either.right(Unit)
    } catch (e: Exception) {
        Either.left(e)
    }
}