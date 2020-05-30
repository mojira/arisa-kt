package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class ResolveTrashModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(project.key, "TRASH").bind()
            resolveAsInvalid()
        }
    }
}
