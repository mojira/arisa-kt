package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import java.time.Instant

const val DESC_DEFAULT = """Put the summary of the bug you're having here

*What I expected to happen was...:*
Describe what you thought should happen here

*What actually happened was...:*
Describe what happened here

*Steps to Reproduce:*
1. Put a step by step guide on how to trigger the bug here
2. ...
3. ..."""
const val ENV_DEFAULT = "Put your operating system (Windows 7, Windows XP, OSX) and Java version if you know it here"
const val MIN_LENGTH = 5

class EmptyModule : Module<EmptyModule.Request> {
    data class Request(
        val created: Instant,
        val lastRun: Instant,
        val numAttachments: Int,
        val description: String?,
        val environment: String?,
        val resolveAsIncomplete: () -> Either<Throwable, Unit>,
        val addEmptyComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = Either.fx {
        with(request) {
            assertGreaterThan(created, lastRun).bind()
            if (description != DESC_DEFAULT && environment != ENV_DEFAULT) {
                assertNotBigger(description, MIN_LENGTH).bind()
                assertNotBigger(environment, MIN_LENGTH).bind()
            } else {
                assertNotEqual(description, DESC_DEFAULT).bind()
                assertNotEqual(environment, ENV_DEFAULT).bind()
            }
            assertNoAttachments(numAttachments).bind()
            addEmptyComment().toFailedModuleEither().bind()
            resolveAsIncomplete().toFailedModuleEither().bind()
        }
    }

    private fun assertNoAttachments(i: Int) = when {
        i != 0 -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertNotBigger(s: String?, size: Int) = when {
        s.isNullOrBlank() -> Unit.right()
        s.length > size -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertNotEqual(s: String?, default: String) = when {
        s.isNullOrBlank() -> Unit.right()
        s != default -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
