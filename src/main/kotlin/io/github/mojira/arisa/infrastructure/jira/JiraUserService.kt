package io.github.mojira.arisa.infrastructure.jira

import io.github.mojira.arisa.domain.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.User
import net.sf.json.JSONObject

class JiraUserService(val jiraClient: JiraClient) : UserService {
    override fun getGroups(username: String) = runBlocking {
        withContext(Dispatchers.IO) {
            // Mojira does not seem to provide any accountIds, hence the endpoint GET /user/groups cannot be used.
            (jiraClient.restClient.get(
                User.getBaseUri() + "user/",
                mapOf(Pair("username", username), Pair("expand", "groups"))
            ) as JSONObject)
                .getJSONObject("groups")
                .getJSONArray("items")
                .map { (it as JSONObject)["name"] as String }
        }
    }
}