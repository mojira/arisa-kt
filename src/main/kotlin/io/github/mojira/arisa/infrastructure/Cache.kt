package io.github.mojira.arisa.infrastructure

open class Cache<V> {
    private val storage = mutableMapOf<String, V>()

    val forEach = storage.values::forEach

    fun get(key: String) = storage.getOrDefault(key, null)
    fun add(key: String, value: V) {
        storage[key] = value
    }

    fun clear() {
        storage.clear()
    }
}
