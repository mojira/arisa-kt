package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.complement
import io.github.mojira.arisa.domain.Version

class FutureVersionModule : Module<FutureVersionModule.Request> {
    data class Request(
        val affectedVersions: List<Version>,
        val versions: List<Version>?,
        val addFutureVersionComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val removeFutureVersions = affectedVersions
                .filter(::isFutureVersion)
                .map { it.execute }
            assertNotEmpty(removeFutureVersions).bind()

            val latestVersion = versions?.lastOrNull(::isFutureVersion.complement())
            assertNotNull(latestVersion).bind()

            latestVersion!!.execute().toFailedModuleEither().bind()
            tryRunAll(removeFutureVersions).bind()
            addFutureVersionComment().toFailedModuleEither().bind()
        }
    }

    private fun isFutureVersion(version: Version) =
        !version.released && !version.archived
}
