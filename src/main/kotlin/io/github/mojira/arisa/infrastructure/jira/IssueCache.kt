package io.github.mojira.arisa.infrastructure.jira

import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient

class IssueCache<T>(val jiraClient: JiraClient, val transformation: (Issue) -> T) {
    val cache = mutableMapOf<String, T>()

    fun clear() = cache.clear()

    fun save(values: Map<String, T>) = cache.putAll(values)

    operator fun set(key: String, value: T) = cache.put(key, value)

    operator fun get(key: String): T {
        return if (cache.containsKey(key)) {
            cache[key]!!
        } else {
            val temp = transformation(jiraClient.getIssue(key, "*all", "changelog"))
            cache[key] = temp
            temp
        }
    }
}