package io.github.mojira.arisa.infrastructure.apiclient

import io.github.mojira.arisa.infrastructure.apiclient.models.IssueBean
import io.github.mojira.arisa.infrastructure.apiclient.models.Project
import io.github.mojira.arisa.infrastructure.apiclient.models.SearchResults
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.IOException
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*

/**
 * Adds authentication headers to the request.
 */
class BasicAuthInterceptor(
    private val email: String,
    private val apiToken: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest: Request = chain.request()

        val credentials = Credentials.basic(email, apiToken)
        val newRequest: Request = originalRequest.newBuilder()
            .header("Authorization", credentials)
            .build()

        return chain.proceed(newRequest)
    }
}

interface JiraApi {
    @GET("project/{key}")
    fun getProject(
        @Path("key") key: String
    ): Call<Project>

    @GET("issue/{key}")
    fun getIssue(
        @Path("key") key: String,
        @Query("fields") fields: String = "*all",
        @Query("expand") expand: String = "changelog"
    ): Call<IssueBean>

    @POST("search")
    fun searchIssues(
        @Body request: JiraSearchRequest
    ): Call<SearchResults>
}

class JiraClient(
    private val jiraUrl: String,
    private val email: String,
    private val apiToken: String
) {
    private val jiraApi: JiraApi

    init {
        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(email, apiToken))
            .build()

        val apiBaseUrl = jiraUrl.plus("rest/api/3/")
        val mediaType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        val retrofit = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()

        jiraApi = retrofit.create(JiraApi::class.java)
    }

    fun searchIssues(
        jql: String,
        fields: List<String> = emptyList(),
        expand: List<String> = emptyList(),
        maxResults: Int = 100,
        startAt: Int = 0,
    ): SearchResults {
        val payload = JiraSearchRequest(
            expand = expand,
            fields = fields,
            jql = jql,
            maxResults = maxResults,
            startAt = startAt
        )

        val response = jiraApi.searchIssues(payload).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }

        return response.body() ?: throw IOException("Empty response body")
    }

    fun getProject(key: String): Project {
        val response = jiraApi.getProject(key).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code ${response.code()}")
        }

        return response.body() ?: throw IOException("Empty response body")
    }

    fun getIssue(
        key: String,
        includedFields: String = "*all",
        expand: String = "changelog",
    ): IssueBean {
        val response = jiraApi.getIssue(key, includedFields, expand).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code ${response.code()}")
        }

        return response.body() ?: throw IOException("Empty response body")
    }

}
