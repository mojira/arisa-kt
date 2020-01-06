package io.github.mojira.arisa.domain.service

import arrow.core.Either
import io.github.mojira.arisa.domain.model.Attachment

interface DeleteAttachmentService {
    fun deleteAttachment(attachment: Attachment): Either<Exception, Unit>
}