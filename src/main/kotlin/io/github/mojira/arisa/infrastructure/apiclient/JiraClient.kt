package io.github.mojira.arisa.infrastructure.apiclient

import io.github.mojira.arisa.infrastructure.apiclient.models.Project
import io.github.mojira.arisa.infrastructure.apiclient.models.SearchResults
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException

private val JSON = "application/json".toMediaType()

/**
 * Adds authentication headers to the request.
 */
class BasicAuthInterceptor(
    private val email: String,
    private val apiToken: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val credentials = Credentials.basic(email, apiToken)
        val newRequest: Request = originalRequest.newBuilder()
            .header("Authorization", credentials)
            .build()

        return chain.proceed(newRequest)
    }
}

class JiraClient(
    private val jiraUrl: String,
    private val email: String,
    private val apiToken: String
) {
    private final val API_ENDPOINT = jiraUrl.plus("rest/api/3")
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor(email, apiToken))
        .build()
    private val JsonWithUnknownKeys = Json{ignoreUnknownKeys = true}

    private fun constructURL(url: String): String {
        return this.API_ENDPOINT.plus(url)
    }

    private fun getRequest(url: String): Response {
        val request = Request.Builder()
            .url(constructURL(url))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return this.httpClient.newCall(request).execute()
    }

    private fun postRequest(url: String, payload: RequestBody): Response {
        val request  = Request.Builder()
            .url(constructURL(url))
            .post(payload)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return this.httpClient.newCall(request).execute()
    }

     fun searchIssues(
         jql: String,
         fields: List<String>,
         expand: List<String> = emptyList(),
         maxResults: Int,
         startAt: Int = 0,
     ): SearchResults {
         val payload = Json.encodeToString(JiraSearchRequest(
             expand = expand,
             fields = fields,
             jql = jql,
             maxResults = maxResults,
             startAt = startAt
         )).toRequestBody(JSON)

         val response = this.postRequest("/search", payload)
         response.use { response ->
             if (!response.isSuccessful) {
                 throw IOException("Unexpected code $response")
             }

             return Json.decodeFromString<SearchResults>(response.body!!.string())
         }
     }

    fun getProject(
        key: String
    ): Project {
        val response = this.getRequest("/project/$key")
        response.use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            return this.JsonWithUnknownKeys.decodeFromString<Project>(response.body!!.string())
        }
    }

}
