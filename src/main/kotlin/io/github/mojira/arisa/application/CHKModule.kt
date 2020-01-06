package io.github.mojira.arisa.application

import arrow.core.Either
import io.github.mojira.arisa.domain.service.UpdateCHKService

class CHKModule(
    val updateCHKService: UpdateCHKService
): Module<CHKModuleRequest> {
    override fun invoke(request: CHKModuleRequest): ModuleResponse = with(request) {
        if (confirmationField != null && confirmationField != "Undefined" && chkField == null) {
            when (val result = updateCHKService.updateCHK(issueId)) {
                is Either.Left -> FailedModuleResponse(listOf(result.a))
                else -> SucessfulModuleResponse
            }
        } else {
            OperationNotNeededModuleResponse
        }
    }
}

data class CHKModuleRequest(val issueId: String, val chkField: String?, val confirmationField: String?)
