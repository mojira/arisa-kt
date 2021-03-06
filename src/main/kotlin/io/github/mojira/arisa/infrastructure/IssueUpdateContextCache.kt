package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import io.github.mojira.arisa.domain.IssueUpdateContext
import io.github.mojira.arisa.infrastructure.jira.applyIssueChanges
import io.github.mojira.arisa.log

/**
 * Temporary helper singleton until we have refactored issue updates in general.
 */
object IssueUpdateContextCache {
    private val cache = Cache<IssueUpdateContext>()

    fun get(key: String): IssueUpdateContext? = cache.get(key)

    fun add(key: String, value: IssueUpdateContext) = cache.add(key, value)

    /**
     * Update `triggeredBy` field of issue update contexts in the cache.
     */
    fun updateTriggeredBy(issue: String) {
        cache.storage.forEach {
            it.value.triggeredBy = it.value.triggeredBy ?: issue
        }
    }

    /**
     * Goes through all cached issue update contexts and applies the changes.
     * Flushes the cache afterwards.
     */
    fun applyChanges(addFailedTicket: (String) -> Any) {
        cache.storage
            .mapValues { Pair(it.value.triggeredBy, applyIssueChanges(it.value)) }
            .filterValues { (_, result) -> result.isLeft() }
            .forEach { (updateTo, pair) ->
                val (triggeredBy, result) = pair
                (result as Either.Left).a.exceptions.forEach {
                    log.error("[UPDATE] [TO $updateTo] [BY $triggeredBy] Failed", it)
                }
                addFailedTicket(triggeredBy!!)
            }
        cache.clear()
    }
}
