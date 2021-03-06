package io.github.mojira.arisa.infrastructure.jira

import io.github.mojira.arisa.infrastructure.services.Cache
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient

class IssueCache<T>(val jiraClient: JiraClient, val transformation: (Issue) -> T) {
    val cache = Cache<T>()

    fun clear() = cache.clear()

    fun save(values: Map<String, T>) = cache.putAll(values)

    operator fun set(key: String, value: T) {
        cache[key] = value
    }

    operator fun get(key: String): T {
        val value = cache[key]
        return if (value != null) {
            value
        } else {
            val temp = transformation(jiraClient.getIssue(key, "*all", "changelog"))
            cache[key] = temp
            temp
        }
    }
}