package io.github.mojira.arisa.infrastructure

import net.rcarz.jiraclient.Issue
import java.util.Timer
import kotlin.concurrent.schedule

class Cache {
    private val ticketCache = mutableListOf<String>()
    private val ticketTimer = Timer("RemoveCachedTicket", true)
    val queryCache = mutableMapOf<String, List<Issue>>()
    val processedTickets = mutableMapOf<String, Boolean>()

    fun addProcessedTickets(timeLimit: Long) {
        processedTickets
            .filter { (_, executed) -> !executed }
            .map { (ticket, _) -> ticket }
            .forEach { ticket ->
                ticketCache.add(ticket)
                ticketTimer.schedule(timeLimit) { ticketCache.remove(ticket) }
            }
    }

    fun isEmpty() = ticketCache.isEmpty()
    fun getTickets() = ticketCache.joinToString(",")
    fun getQuery(combinedJql: String) = queryCache.getOrDefault(combinedJql, null)
    fun addQuery(combinedJql: String, issues: List<Issue>) {
        queryCache[combinedJql] = issues
    }

    fun startProcessingTicket(issue: String) {
        processedTickets.putIfAbsent(issue, false)
    }

    fun finishProcessingTicket(issue: String) {
        processedTickets[issue] = true
    }

    fun clearQueryCache() {
        queryCache.clear()
    }
}
