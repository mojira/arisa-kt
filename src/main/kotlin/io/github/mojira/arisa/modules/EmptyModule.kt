package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

const val DESCDEFAULT = "Put the summary of the bug you're having here\r\n\r\n*What I expected to happen was...:*\r\nDescribe what you thought should happen here\r\n\r\n*What actually happened was...:*\r\nDescribe what happened here\r\n\r\n*Steps to Reproduce:*\r\n1. Put a step by step guide on how to trigger the bug here\r\n2. ...\r\n3. ..."
const val ENVDEFAULT = "Put your operating system (Windows 7, Windows XP, OSX) and Java version if you know it here"
const val MINLENGTH = 5

class EmptyModule : Module<EmptyModule.Request> {
    data class Request(
        val numAttachments: Int,
        val description: String?,
        val environment: String?,
        val resolveAsIncomplete: () -> Either<Throwable, Unit>,
        val addEmptyComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = Either.fx {
        with(request) {
            if (description != DESCDEFAULT && environment != ENVDEFAULT) {
                assertNotBigger(description, MINLENGTH).bind()
                assertNotBigger(environment, MINLENGTH).bind()
            } else {
                assertNotEqual(description, DESCDEFAULT).bind()
                assertNotEqual(environment, ENVDEFAULT).bind()
            }
            assertNoAttachments(numAttachments).bind()
            addEmptyComment().toFailedModuleEither().bind()
            resolveAsIncomplete().toFailedModuleEither().bind()
        }
    }

    fun assertNoAttachments(i: Int) = when {
        i != 0 -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    fun assertNotBigger(s: String?, size: Int) = when {
        s.isNullOrBlank() -> Unit.right()
        s.length > size -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    fun assertNotEqual(s: String?, default: String) = when {
        s.isNullOrBlank() -> Unit.right()
        s != default -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
