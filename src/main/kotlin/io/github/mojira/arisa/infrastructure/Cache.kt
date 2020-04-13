package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue

class Cache {
    private val queryCache = mutableMapOf<String, Sequence<Issue>>()

    fun getQuery(combinedJql: String) = queryCache.getOrDefault(combinedJql, null)
    fun addQuery(combinedJql: String, issues: Sequence<Issue>) {
        queryCache[combinedJql] = issues
    }

    fun clearQueryCache() {
        queryCache.clear()
    }
}
