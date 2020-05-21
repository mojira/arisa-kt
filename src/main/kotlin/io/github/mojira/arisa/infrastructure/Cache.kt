package io.github.mojira.arisa.infrastructure

@Suppress("MemberNameEqualsClassName")
open class Cache<V> {
    private val cache = mutableMapOf<String, V>()

    val forEach = cache.values::forEach

    fun get(key: String) = cache.getOrDefault(key, null)
    fun add(key: String, value: V) {
        cache[key] = value
    }

    fun clear() {
        cache.clear()
    }
}
