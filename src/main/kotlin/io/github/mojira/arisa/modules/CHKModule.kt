package io.github.mojira.arisa.modules

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.Arisa
import net.rcarz.jiraclient.JiraClient
import java.time.Instant

class CHKModule(jiraClient: JiraClient, config: Config) : Module<CHKModuleRequest>(jiraClient, config) {
    override fun invoke(request: CHKModuleRequest): ModuleResponse = with(request) {
        if (confirmationField != null && confirmationField != "Undefined" && chkField == null) {
            when (val result = updateCHK(issueId)) {
                is Either.Left -> FailedModuleResponse(listOf(result.a))
                else -> SucessfulModuleResponse
            }
        } else {
            OperationNotNeededModuleResponse
        }
    }

    private fun updateCHK(issueId: String): Either<Exception, Unit> = try {
        jiraClient
            .getIssue(issueId)
            .update()
            .field(config[Arisa.CustomFields.chkField], Instant.now().toString())
            .execute()
        Either.right(Unit)
    } catch (e: Exception) {
        Either.left(e)
    }

}

data class CHKModuleRequest(val issueId: String, val chkField: String?, val confirmationField: String?)
