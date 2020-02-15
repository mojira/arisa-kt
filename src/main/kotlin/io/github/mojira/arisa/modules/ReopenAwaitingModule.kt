package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Resolution
import java.util.Date

data class ReopenAwaitingModuleRequest(
    val resolution: Resolution?,
    val created: Date,
    val updated: Date,
    val comments: List<Comment>
)

class ReopenAwaitingModule : Module<ReopenAwaitingModuleRequest> {
    override fun invoke(request: ReopenAwaitingModuleRequest): Either<ModuleError, ModuleResponse> {
        with(request) {
            if (resolution == null || resolution.name != "Awaiting Response" || (updated.time - created.time) < 2000 || comments.isEmpty()) {
                return OperationNotNeededModuleResponse.left()
            }
        }

        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
