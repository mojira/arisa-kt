package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue

class QueryCache {
    private val queryCache = mutableMapOf<String, List<Issue>>()

    fun getQuery(combinedJql: String) = queryCache.getOrDefault(combinedJql, null)
    fun addQuery(combinedJql: String, issues: List<Issue>) {
        queryCache[combinedJql] = issues
    }

    fun clearQueryCache() {
        queryCache.clear()
    }
}
