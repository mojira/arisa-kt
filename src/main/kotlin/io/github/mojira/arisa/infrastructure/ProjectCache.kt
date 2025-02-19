package io.github.mojira.arisa.infrastructure

import io.github.mojira.arisa.infrastructure.apiclient.models.Project
import io.github.mojira.arisa.jiraClient
import java.time.Duration
import java.time.Instant
//import net.rcarz.jiraclient.Project as JiraProject

object ProjectCache {
    private var cache = Cache<Project>()
    private var lastRefresh = Instant.now()

    private const val REFRESH_INTERVAL_IN_MINUTES: Long = 5

    fun getProjectFromTicketId(id: String): Project {
        checkForRefresh()

        val projectKey = id.split("-")[0]
        return cache.get(projectKey) ?: run {
            val jiraProject = jiraClient.getProject(projectKey)
            cache.add(projectKey, jiraProject)
            jiraProject
        }
    }

    private fun checkForRefresh() {
        // It would be nice if we could perform a less expensive API call here,
        // so that we could refresh more often than only every 5 minutes...
        // However, our JIRA library doesn't have an easy way
        // to access the `/project/<key>/version` API. Sad times.

        if (Duration.between(lastRefresh, Instant.now()).toMinutes() >= REFRESH_INTERVAL_IN_MINUTES) {
            cache.clear()
            lastRefresh = Instant.now()
        }
    }

    fun forceClear() {
        cache.clear()
    }
}
