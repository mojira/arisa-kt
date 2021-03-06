@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

fun <T> Either<Throwable, T>.toFailedModuleEither(issue: Issue) = this.bimap(
    { FailedModuleResponse(issue, listOf(it)) },
    { it }
)

fun Collection<Either<Throwable, Any>>.toFailedModuleEither(issue: Issue): Either<ModuleError, ModuleResponse> {
    val errors =
        filter(Either<Throwable, Any>::isLeft)
            .map { (it as Either.Left).a }

    return if (errors.isEmpty()) {
        ModuleResponse(issue).right()
    } else {
        FailedModuleResponse(issue, errors).left()
    }
}

fun Either<OperationNotNeededModuleResponse, ModuleResponse>.invert() = fold(
    { ModuleResponse(it.issue).right() },
    { OperationNotNeededModuleResponse(it.issue).left() }
)

fun assertContains(original: String?, match: String, issue: Issue) = when {
    original.isNullOrEmpty() -> OperationNotNeededModuleResponse(issue).left()
    original.contains(match, true) -> ModuleResponse(issue).right()
    else -> OperationNotNeededModuleResponse(issue).left()
}

fun assertEmpty(c: Collection<*>, issue: Issue) = when {
    c.isEmpty() -> ModuleResponse(issue)
    else -> OperationNotNeededModuleResponse(issue).left()
}

fun assertNotEmpty(c: Collection<*>, issue: Issue) = when {
    c.isEmpty() -> OperationNotNeededModuleResponse(issue).left()
    else -> ModuleResponse(issue).right()
}

fun <T> assertNull(e: T?, issue: Issue) = when (e) {
    null -> ModuleResponse(issue).right()
    else -> OperationNotNeededModuleResponse(issue).left()
}

fun <T> assertNotNull(e: T?, issue: Issue) = when (e) {
    null -> OperationNotNeededModuleResponse(issue).left()
    else -> ModuleResponse(issue).right()
}

fun assertEither(issue: Issue, vararg list: Either<OperationNotNeededModuleResponse, ModuleResponse>) =
    if (list.any { it.isRight() }) {
        ModuleResponse(issue).right()
    } else {
        OperationNotNeededModuleResponse(issue).left()
    }

/**
 * The functions will stop executing when any of them fails. Therefore, we can avoid the bot from spamming comments
 * when the resolve fails in case it needs to resolve and comment
 */
fun tryRunAll(
    functions: Collection<() -> Either<Throwable, Unit>>,
    issue: Issue
): Either<FailedModuleResponse, ModuleResponse> {
    functions.forEach {
        val result = it()
        if (result.isLeft()) {
            return FailedModuleResponse(issue, listOf((result as Either.Left).a)).left()
        }
    }

    return ModuleResponse(issue).right()
}

fun <T> assertEquals(o1: T, o2: T, issue: Issue) = if (o1 == o2) {
    ModuleResponse(issue).right()
} else {
    OperationNotNeededModuleResponse(issue).left()
}

fun assertTrue(b: Boolean, issue: Issue) = if (b) {
    ModuleResponse(issue).right()
} else {
    OperationNotNeededModuleResponse(issue).left()
}

fun assertFalse(b: Boolean, issue: Issue) = if (!b) {
    ModuleResponse(issue).right()
} else {
    OperationNotNeededModuleResponse(issue).left()
}

fun <T> assertNotEquals(o1: T, o2: T, issue: Issue) = if (o1 == o2) {
    OperationNotNeededModuleResponse(issue).left()
} else {
    ModuleResponse(issue).right()
}

fun <T : Comparable<T>> assertGreaterThan(o1: T, o2: T, issue: Issue) = if (o1 > o2) {
    ModuleResponse(issue).right()
} else {
    OperationNotNeededModuleResponse(issue).left()
}

fun assertAfter(instant1: Instant, instant2: Instant, issue: Issue) = if (instant1.isAfter(instant2)) {
    ModuleResponse(issue).right()
} else {
    OperationNotNeededModuleResponse(issue).left()
}

fun String?.getOrDefault(default: String) =
    if (isNullOrBlank())
        default
    else
        this

fun MutableList<String>.splitElemsByCommas() {
    val newList = this.flatMap { s ->
        s.split(',').filter {
            it.isNotEmpty()
        }
    }
    this.clear()
    this.addAll(newList)
}

fun String.isTicketKey(): Boolean {
    return this.matches(Regex("[A-Za-z]+-[0-9]+"))
}

fun String.isTicketLink(): Boolean {
    return this.matches(Regex("https://bugs.mojang.com/browse/[A-Z]+-[0-9]+"))
}

const val MAX_LINK_TYPE_LENGTH = 3

fun MutableList<String>.concatLinkName() {
    val tmpList = this.take(MAX_LINK_TYPE_LENGTH + 1)
    val linkNameList = tmpList.takeWhile {
        !(it.isTicketKey() || it.isTicketLink())
    }
    if (linkNameList.size == tmpList.size) {
        this.add(0, "")
        return
    }
    val linkName = linkNameList.joinToString(separator = " ")
    val newList = if (linkName.isEmpty()) this.toMutableList()
    else this.drop(linkName.count { it == ' ' } + 1).toMutableList()
    newList.add(0, linkName)
    this.clear()
    this.addAll(newList)
}

fun MutableList<String>.convertLinks() {
    val newList = mutableListOf<String>()
    for (arg in this) {
        var key = arg
        if (key.isTicketLink()) {
            key = key.drop("https://bugs.mojang.com/browse/".length)
        }
        newList.add(newList.size, key)
    }
    this.clear()
    this.addAll(newList)
}

data class LinkType(
    val name: List<String>,
    val id: String,
    val outwards: Boolean
) {
    val nameVariants = concatenateCombinations(name)
}

val linkTypes = listOf(
    LinkType(listOf("relates", "to"), "Relates", true),
    LinkType(listOf("duplicates"), "Duplicate", true),
    LinkType(listOf("is", "duplicated", "by"), "Duplicate", false),
    LinkType(listOf("clones"), "Cloners", true),
    LinkType(listOf("is", "cloned", "by"), "Cloners", false),
    LinkType(listOf("blocks"), "Blocks", true),
    LinkType(listOf("is", "blocked", "by"), "Blocks", false),
    LinkType(listOf("testing", "discovered"), "Bonfire Testing", true),
    LinkType(listOf("discovered", "while", "testing"), "Bonfire Testing", false)
)

private fun concatenateCombinations(list: List<String>): Set<String> {
    val newSet = mutableSetOf<String>()
    list.forEachIndexed { index, word ->
        newSet.add(word)
        if (index < list.size - 1) {
            var combination = word
            for (item in list.subList(index + 1, list.size)) {
                combination = "$combination $item"
                newSet.add(combination)
            }
        }
    }
    return newSet.toSortedSet()
}

fun addLinks(issue: Issue, type: String, keys: List<String>): Either<ModuleError, ModuleResponse> = Either.fx {
    val tmp = linkTypes.filter {
        type.toLowerCase() in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    for (key in keys) {
        issue.createLink(linkType.id, key.toUpperCase(), linkType.outwards)
    }
}

fun deleteLinks(issue: Issue, type: String, keys: List<String>): Either<ModuleError, ModuleResponse> = Either.fx {
    val tmp = linkTypes.filter {
        type.toLowerCase() in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    for (key in keys) {
        val link = issue.links.find {
            it.type == linkType.id && it.issue.key == key.toUpperCase()
        }
        assertNotNull(link).bind()
        link?.remove?.invoke()
    }
}
