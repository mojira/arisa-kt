package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.Version

data class FutureVersionModuleRequest(
    val issue: Issue,
    val affectedVersions: List<Version>,
    val versions: List<Version>?
)

class FutureVersionModule(
    val removeVersion: (Issue, Version) -> Either<Throwable, Unit>,
    val addVersion: (Issue, Version) -> Either<Throwable, Unit>,
    val addFutureVersionComment: (Issue) -> Either<Throwable, Unit>
) : Module<FutureVersionModuleRequest> {

    override fun invoke(request: FutureVersionModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val futureVersions = affectedVersions.filter(::isFutureVersion)
            val latestVersion = versions?.lastOrNull { !isFutureVersion(it) }
            assertNotEmpty(futureVersions).bind()
            assertNotNull(latestVersion).bind()

            addVersion(issue, latestVersion!!).toFailedModuleEither().bind()
            tryRunAll(removeVersion.partially1(issue), futureVersions).bind()
            addFutureVersionComment(issue).toFailedModuleEither().bind()
        }
    }

    private fun isFutureVersion(version: Version) =
        !version.isReleased && !version.isArchived
}
