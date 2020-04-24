package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue

class QueryCache {
    private val queryCache = mutableMapOf<String, List<Issue>>()

    fun get(combinedJql: String) = queryCache.getOrDefault(combinedJql, null)
    fun add(combinedJql: String, issues: List<Issue>) {
        queryCache[combinedJql] = issues
    }

    fun clear() {
        queryCache.clear()
    }
}
