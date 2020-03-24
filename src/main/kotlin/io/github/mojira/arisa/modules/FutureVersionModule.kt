package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.Version

data class FutureVersionModuleRequest(
    val affectedVersions: List<Version>,
    val versions: List<Version>?
)

class FutureVersionModule(
    val removeVersion: (Version) -> Either<Throwable, Unit>,
    val addVersion: (Version) -> Either<Throwable, Unit>,
    val addFutureVersionComment: () -> Either<Throwable, Unit>
) : Module<FutureVersionModuleRequest> {

    override fun invoke(request: FutureVersionModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val futureVersions = request.affectedVersions.filter(::isFutureVersion)
        val latestVersion = request.versions?.lastOrNull { !isFutureVersion(it) }
        assertNotEmpty(futureVersions).bind()
        assertNotNull(latestVersion).bind()

        addFutureVersionComment().toFailedModuleEither().bind()
        tryRunAll(removeVersion, futureVersions).bind()
        addVersion(latestVersion!!).toFailedModuleEither().bind()
    }

    private fun isFutureVersion(version: Version) =
        !version.isReleased && !version.isArchived
}
