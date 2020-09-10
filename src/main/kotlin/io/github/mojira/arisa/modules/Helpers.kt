@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

fun <T> Either<Throwable, T>.toFailedModuleEither() = this.bimap(
    { FailedModuleResponse(listOf(it)) },
    { it }
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

fun concatLinkName(vararg arguments: String): Array<String> {
    val args = arguments.flatMap { s ->
        s.split(',').filter {
            it != ""
        }
    }.toTypedArray()
    fun check(str: String): Boolean {
        return !str.matches(Regex("[A-Z]+-[0-9]+")) &&
                !str.matches(Regex("https://bugs.mojang.com/browse/[A-Z]+-[0-9]+"))
    }
    if (check(args[0])) {
        val result = mutableListOf(*args)
        result.add(0, "")
        return result.toTypedArray()
    }
    var linkName = args[0]
    var iSaved = 0
    for (i in args.indices) {
        iSaved = i
        if (!check(args[i]))
            break
        val word = args[i]
        linkName = "$linkName $word"
    }
    val result = mutableListOf(*args).subList(iSaved, arguments.size)
    result.add(0, linkName)
    return result.toTypedArray()
}

fun convertLinks(vararg arguments: String): Array<String> {
    val newArgList = mutableListOf<String>()
    for (arg in arguments) {
        var key = arg
        if (key.matches(Regex("https://bugs.mojang.com/browse/[A-Z]+-[0-9]+"))) {
            key = key.drop(31)
        }
        newArgList.add(newArgList.size, key)
    }
    return newArgList.toTypedArray()
}

data class LinkType(
    val name: List<String>,
    val id: String,
    val outwards: Boolean
) {
    lateinit var nameVariants: Set<String>
    operator fun invoke() {
        nameVariants = concatenateCombinations(name)
    }
}

val linkTypes = listOf(
        LinkType(listOf("relates", "to"), "Relates", true),
        LinkType(listOf("duplicates"), "Duplicate", true),
        LinkType(listOf("is","duplicated", "by"), "Duplicate", false),
        LinkType(listOf("clones"), "Cloners", true),
        LinkType(listOf("is", "cloned", "by"), "Cloners", false),
        LinkType(listOf("blocks"), "Blocks", true),
        LinkType(listOf("is", "blocked", "by"), "Blocks", false),
        LinkType(listOf("testing", "discovered"), "Bonfire Testing", true),
        LinkType(listOf("discovered", "while", "testing"), "Bonfire Testing", false)
)

private fun concatenateCombinations(list : List<String>): Set<String> {
    val newList = mutableListOf<String>()
    for (word in list) {
        val tmp = mutableListOf<String>()
        for (item in newList) {
            tmp.add("$item $word")
        }
        newList.addAll(tmp)
        newList.add(word)
    }
    return newList.toSet()
}

fun addLinks(issue: Issue, type: String, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
    val tmp = linkTypes.filter {
        type in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    assertTrue(linkType.outwards).bind()
    for (key in arguments) {
        issue.createLink(linkType.id, key)
    }
}

fun deleteLinks(issue: Issue, type: String, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
    val tmp = linkTypes.filter {
        type in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    for (key in arguments) {
        assertTrue(key.matches(Regex("[A-Z]+-[0-9]+"))).bind()
    }
    for (key in arguments) {
        val link = issue.links.find {
            it.type == linkType.id && it.issue.key == key && (linkType.id == "Relates" || it.outwards == linkType.outwards)
        }
        assertNotNull(link).bind()
        link?.remove?.invoke()
    }
}