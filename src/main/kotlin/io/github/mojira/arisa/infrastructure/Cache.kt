package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue

class Cache(
    private var failedTickets: Set<String>
) {
    private val queryCache = mutableMapOf<String, List<Issue>>()
    private val failedTicketsCurRun = mutableSetOf<String>()

    fun getQuery(combinedJql: String) = queryCache.getOrDefault(combinedJql, null)
    fun addQuery(combinedJql: String, issues: List<Issue>) {
        queryCache[combinedJql] = issues
    }

    fun updatedFailedTickets() {
        failedTickets = failedTicketsCurRun
    }

    fun addFailedTicket(ticket: String) {
        failedTicketsCurRun.add(ticket)
    }

    fun getFailedTickets(): Set<String> =
        failedTickets

    fun clearQueryCache() {
        queryCache.clear()
    }
}
