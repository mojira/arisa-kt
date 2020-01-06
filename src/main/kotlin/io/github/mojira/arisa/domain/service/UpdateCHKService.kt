package io.github.mojira.arisa.domain.service

import arrow.core.Either

interface UpdateCHKService {
    fun updateCHK(issueId: String): Either<Exception, Unit>
}
