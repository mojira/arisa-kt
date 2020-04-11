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

fun assertNotEmpty(c: Collection<*>) = when {
    c.isEmpty() -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun <T> assertNotNull(e: T?) = when (e) {
    null -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun tryRunAll(
    functs: List<() -> Either<Throwable, Unit>>
): Either<FailedModuleResponse, ModuleResponse> {
    val exceptions = functs
        .map { it() }
        .filter { it.isLeft() }
        .map { (it as Either.Left).a }

    return if (exceptions.isEmpty()) {
        ModuleResponse.right()
    } else {
        FailedModuleResponse(exceptions).left()
    }
}

fun <T> assertEquals(o1: T, o2: T) = if (o1 == o2) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}

fun <T> assertNotEquals(o1: T, o2: T) = if (o1 == o2) {
    OperationNotNeededModuleResponse.left()
} else {
    Unit.right()
}
