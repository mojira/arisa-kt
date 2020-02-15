package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import net.rcarz.jiraclient.Resolution
import java.util.Date

data class ReopenAwaitingModuleRequest(val resolution: Resolution?, val created: Date, val updated: Date)

class ReopenAwaitingModule : Module<ReopenAwaitingModuleRequest> {
    override fun invoke(request: ReopenAwaitingModuleRequest): Either<ModuleError, ModuleResponse> {
        if (request.resolution == null || request.resolution.name != "Awaiting Response") {
            return OperationNotNeededModuleResponse.left()
        }

        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
