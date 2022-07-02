@file:Suppress("TooManyFunctions")

package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

fun assertAny(vararg list: Either<OperationNotNeededModuleResponse, ModuleResponse>) =
    if (list.any { it.isRight() }) {
        Unit.right()
    } else {
        OperationNotNeededModuleResponse.left()
    }

/**
 * The functions will stop executing when any of them fails. Therefore, we can avoid the bot from spamming comments
 * when the resolve fails in case it needs to resolve and comment
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

fun String?.getOrDefault(default: String) =
    if (isNullOrBlank())
        default
    else
        this

fun String?.getOrDefaultNull(default: String) =
    this ?: default

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
        type.lowercase() in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    for (key in keys) {
        issue.createLink(linkType.id, key.uppercase(), linkType.outwards)
    }
}

fun deleteLinks(issue: Issue, type: String, keys: List<String>): Either<ModuleError, ModuleResponse> = Either.fx {
    val tmp = linkTypes.filter {
        type.lowercase() in it.nameVariants
    }
    assertNotNull(tmp).bind()
    assertTrue(tmp.size == 1).bind()
    val linkType = tmp[0]
    for (key in keys) {
        val link = issue.links.find {
            it.type == linkType.id && it.issue.key == key.uppercase()
        }
        assertNotNull(link).bind()
        link?.remove?.invoke()
    }
}

fun openHttpGetInputStream(uri: URI): InputStream {
    val request = HttpRequest.newBuilder()
        .uri(uri)
        .GET()
        .build()
    return openHttpInputStream(request)
}

private const val HTTP_STATUS_OK = 200

fun openHttpInputStream(request: HttpRequest): InputStream {
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
    val bodyHandler = HttpResponse.BodyHandler { responseInfo ->
        if (responseInfo.statusCode() == HTTP_STATUS_OK) {
            HttpResponse.BodySubscribers.ofInputStream()
        } else {
            // Discard response
            HttpResponse.BodySubscribers.replacing(null)
        }
    }

    val response = client.send(request, bodyHandler)
    return response.body()
        ?: throw IOException(
            "${request.method()} request to ${request.uri()} failed with HTTP status code " +
                    "${response.statusCode()}"
        )
}
