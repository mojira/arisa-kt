package io.github.mojira.arisa.infrastructure

open class Cache<V> {
    val storage = mutableMapOf<String, V>()

    fun get(key: String) = storage.getOrDefault(key, null)
    fun getOrAdd(key: String, defaultValue: V) = storage.getOrPut(key, { defaultValue })

    fun add(key: String, value: V) {
        storage[key] = value
    }

    fun clear() {
        storage.clear()
    }
}
