package io.github.mojira.arisa.infrastructure.newjira

import io.github.mojira.arisa.infrastructure.escapeIssueFunction
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException
import net.sf.json.JSONObject

class UserService(private val jiraClient: JiraClient) {
    fun getGroups(name: String): List<String> = (
        jiraClient.restClient.get(
            JiraUser.getBaseUri() + "user/", mapOf(Pair("username", name), Pair("expand", "groups"))
        ) as JSONObject)
        .getJSONObject("groups")
        .getJSONArray("items")
        .map { (it as JSONObject)["name"] as String }

    fun isNewUser(name: String): Boolean {
        val commentJql = "issueFunction IN commented(${escapeIssueFunction(name) { "by $it before -24h" }})"

        val oldCommentsExist = try {
            jiraClient.countIssues(commentJql) > 0
        } catch (_: JiraException) {
            false
        }

        if (oldCommentsExist) return false

        val reportJql = """project != TRASH AND reporter = '${name.replace("'", "\\'")}' AND created < -24h"""

        val oldReportsExist = try {
            jiraClient.countIssues(reportJql) > 0
        } catch (_: JiraException) {
            true
        }

        return !oldReportsExist
    }
}
