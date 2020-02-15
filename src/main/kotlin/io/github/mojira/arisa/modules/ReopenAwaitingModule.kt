package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import net.rcarz.jiraclient.Resolution
import java.util.Date

data class ReopenAwaitingModuleRequest(val resolution: Resolution?, val created: Date, val updated: Date)

class ReopenAwaitingModule : Module<ReopenAwaitingModuleRequest> {
    override fun invoke(request: ReopenAwaitingModuleRequest): Either<ModuleError, ModuleResponse> {
        with(request) {
            if (resolution == null || resolution.name != "Awaiting Response" || (updated.time - created.time) < 2000) {
                return OperationNotNeededModuleResponse.left()
            }
        }

        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
