package io.github.mojira.arisa.infrastructure.services

open class Cache<V> {
    val storage = mutableMapOf<String, V>()

    operator fun get(key: String) = storage.getOrDefault(key, null)
    fun getOrAdd(key: String, defaultValue: V) = storage.getOrPut(key, { defaultValue })

    operator fun set(key: String, value: V) {
        storage[key] = value
    }

    fun clear() {
        storage.clear()
    }

    fun putAll(values: Map<String, V>) {
        storage.putAll(values)
    }
}
