@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.time.Instant

fun Either<Throwable, Unit>.toFailedModuleEither() = this.bimap(
    { FailedModuleResponse(listOf(it)) },
    { ModuleResponse }
)

fun Collection<Either<Throwable, Any>>.toFailedModuleEither(): Either<ModuleError, ModuleResponse> {
    val errors =
        filter(Either<Throwable, Any>::isLeft)
            .map { (it as Either.Left).a }

    return if (errors.isEmpty()) {
        ModuleResponse.right()
    } else {
        FailedModuleResponse(errors).left()
    }
}

fun Either<OperationNotNeededModuleResponse, ModuleResponse>.invert() =
    if (isLeft()) {
        Unit.right()
    } else {
        OperationNotNeededModuleResponse.left()
    }

fun assertContains(original: String?, match: String) = when {
    original.isNullOrEmpty() -> OperationNotNeededModuleResponse.left()
    original.contains(match, true) -> Unit.right()
    else -> OperationNotNeededModuleResponse.left()
}

fun assertEmpty(c: Collection<*>) = when {
    c.isEmpty() -> Unit.right()
    else -> OperationNotNeededModuleResponse.left()
}

fun assertNotEmpty(c: Collection<*>) = when {
    c.isEmpty() -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun <T> assertNull(e: T?) = when (e) {
    null -> Unit.right()
    else -> OperationNotNeededModuleResponse.left()
}

fun <T> assertNotNull(e: T?) = when (e) {
    null -> OperationNotNeededModuleResponse.left()
    else -> Unit.right()
}

fun assertEither(vararg list: Either<OperationNotNeededModuleResponse, ModuleResponse>) =
    if (list.any { it.isRight() }) {
        Unit.right()
    } else {
        OperationNotNeededModuleResponse.left()
    }

/**
 * The functions will stop executing when any of them fails. Therefore, we can avoid the bot from spamming comments
 * when the resolve fails in case it needs to resolve and comment.
 */
fun tryRunAll(
    functions: Collection<() -> Either<Throwable, Unit>>
): Either<FailedModuleResponse, ModuleResponse> {
    functions.forEach {
        val result = it()
        if (result.isLeft()) {
            return FailedModuleResponse(listOf((result as Either.Left).a)).left()
        }
    }

    return ModuleResponse.right()
}

fun <T> assertEquals(o1: T, o2: T) = if (o1 == o2) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}

fun assertTrue(b: Boolean) = if (b) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}

fun assertFalse(b: Boolean) = if (!b) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}

fun <T> assertNotEquals(o1: T, o2: T) = if (o1 == o2) {
    OperationNotNeededModuleResponse.left()
} else {
    Unit.right()
}

fun <T : Comparable<T>> assertGreaterThan(o1: T, o2: T) = if (o1 > o2) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}

fun assertAfter(instant1: Instant, instant2: Instant) = if (instant1.isAfter(instant2)) {
    Unit.right()
} else {
    OperationNotNeededModuleResponse.left()
}
