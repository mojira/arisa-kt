package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.Version

data class FutureVersionModuleRequest(
    val affected: List<Version>
)

class FutureVersionModule(
    val removeVersion: (Version) -> Either<Throwable, Unit>,
    val addLatestVersion: () -> Either<Throwable, Unit>,
    val addFutureVersionComment: () -> Either<Throwable, Unit>
) : Module<FutureVersionModuleRequest> {

    override fun invoke(request: FutureVersionModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val futureVersions = request.affected.filter(::isFutureVersion)
        assertNotEmpty(futureVersions).bind()

        addFutureVersionComment().toFailedModuleEither().bind()
        tryRunAll(removeVersion, futureVersions).bind()
        addLatestVersion().toFailedModuleEither().bind()
    }

    private fun isFutureVersion(version: Version) =
        !version.isReleased && !version.isArchived
}
