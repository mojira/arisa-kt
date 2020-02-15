package io.github.mojira.arisa.modules

import arrow.core.Either
import net.rcarz.jiraclient.Resolution

data class ReopenAwaitingModuleRequest(val resolution: Resolution?)

class ReopenAwaitingModule : Module<ReopenAwaitingModuleRequest> {
    override fun invoke(request: ReopenAwaitingModuleRequest): Either<ModuleError, ModuleResponse> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
