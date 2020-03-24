package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right

interface Module<REQUEST> {
    operator fun invoke(request: REQUEST): Either<ModuleError, ModuleResponse>
}

typealias ModuleResponse = Unit

sealed class ModuleError
object OperationNotNeededModuleResponse : ModuleError()
data class FailedModuleResponse(val exceptions: List<Throwable> = emptyList()) : ModuleError()

fun Either<Throwable, Unit>.toFailedModuleEither() = this.bimap(
    { FailedModuleResponse(listOf(it)) },
    { ModuleResponse }
)

fun <T> assertNotEmpty(c: Collection<T>) = when {
    c.isEmpty() -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun <T> assertNotNull(e: T?) = when (e) {
    null -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun <T> tryRunAll(
    func: (T) -> Either<Throwable, Unit>,
    elements: Collection<T>
): Either<FailedModuleResponse, ModuleResponse> {
    val exceptions = elements
        .map(func)
        .filter { it.isLeft() }
        .map { (it as Either.Left).a }

    return if (exceptions.isEmpty()) {
        ModuleResponse.right()
    } else {
        FailedModuleResponse(exceptions).left()
    }
}
