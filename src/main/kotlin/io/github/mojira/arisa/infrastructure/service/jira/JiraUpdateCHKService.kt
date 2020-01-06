package io.github.mojira.arisa.infrastructure.service.jira

import arrow.core.Either
import io.github.mojira.arisa.domain.service.UpdateCHKService
import io.github.mojira.arisa.infrastructure.CHKFIELD
import net.rcarz.jiraclient.JiraClient
import java.time.Instant

class JiraUpdateCHKService(
    val jiraClient: JiraClient
): UpdateCHKService {
    override fun updateCHK(issueId: String): Either<Exception, Unit> = try {
        jiraClient
            .getIssue(issueId)
            .update()
            .field(CHKFIELD, Instant.now().toString())
            .execute()
        Either.right(Unit)
    } catch (e: Exception) {
        Either.left(e)
    }

}