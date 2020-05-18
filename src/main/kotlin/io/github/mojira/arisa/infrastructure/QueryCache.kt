package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue

class QueryCache {
    private val cache = mutableMapOf<String, List<Issue>>()

    fun get(combinedJql: String) = cache.getOrDefault(combinedJql, null)
    fun add(combinedJql: String, issues: List<Issue>) {
        cache[combinedJql] = issues
    }

    fun clear() {
        cache.clear()
    }
}
