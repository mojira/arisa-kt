@file:Suppress("ktlint")

package io.github.mojira.arisa.infrastructure.apiclient

import io.github.mojira.arisa.infrastructure.apiclient.exceptions.JiraClientException
import io.github.mojira.arisa.infrastructure.apiclient.exceptions.ClientErrorException
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueBean
import io.github.mojira.arisa.infrastructure.apiclient.models.Project
import io.github.mojira.arisa.infrastructure.apiclient.models.SearchResults
import io.github.mojira.arisa.infrastructure.apiclient.models.Attachment
import io.github.mojira.arisa.infrastructure.apiclient.models.BodyType
import io.github.mojira.arisa.infrastructure.apiclient.models.Comment
import io.github.mojira.arisa.infrastructure.apiclient.models.GroupName
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueLink
import io.github.mojira.arisa.infrastructure.apiclient.models.User
import io.github.mojira.arisa.infrastructure.apiclient.models.Visibility
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.AddCommentBody
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.CreateIssueLinkBody
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.EditIssueBody
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.JiraSearchRequest
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.UpdateCommentBody
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import okio.IOException
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Part
import retrofit2.http.DELETE
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PUT
import java.io.File
import java.io.InputStream

/**
 * Adds authentication headers to the request.
 */
class BasicAuthInterceptor(
    private val email: String,
    private val apiToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val credentials = Credentials.basic(email, apiToken)
        val newRequest: Request =
            originalRequest
                .newBuilder()
                .header("Authorization", credentials)
                .build()

        return chain.proceed(newRequest)
    }
}

interface JiraApi {
    @GET("project/{key}")
    fun getProject(
        @Path("key") key: String,
    ): Call<Project>

    @GET("issue/{key}")
    fun getIssue(
        @Path("key") key: String,
        @Query("fields") fields: String = "*all",
        @Query("expand") expand: String = "changelog",
    ): Call<IssueBean>

    @PUT("issue/{key}")
    fun editIssue(
        @Path("key") key: String,
        @Body body: EditIssueBody,
    ): Call<Unit>

    @GET("myself")
    fun getCurrentUser(
        @Query("expand") expand: String?
    ): Call<User>

    @GET("user/groups")
    fun getUserGroups(
        @Query("accountId") accountsId: String,
        @Query("username") username: String?,
        @Query("key") key: String?
    ): Call<List<GroupName>>

    @POST("search")
    fun searchIssues(
        @Body request: JiraSearchRequest,
    ): Call<SearchResults>

    @GET("attachment/content/{id}")
    fun downloadAttachment(
        @Path("id") attachmentId: String
    ): Call<ResponseBody>

    @Multipart
    @Headers("X-Atlassian-Token: no-check")
    @POST("issue/{issueIdOrKey}/attachments")
    fun addAttachment(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Part file: MultipartBody.Part
    ): Call<List<Attachment>>

    @DELETE("attachment/{id}")
    fun deleteAttachment(
        @Path("id") attachmentId: String
    ): Call<Void>

    @POST("issue/{issueIdOrKey}/comment")
    fun addComment(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Query("expand") expand: String? = null,
        @Body body: AddCommentBody
    ): Call<Comment>

    @PUT("issue/{issueIdOrKey}/comment/{id}")
    fun updateComment(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Path("id") commentId: String,
        @Query("notifyUsers") notifyUsers: Boolean? = null,
        @Query("overrideEditableFlag") overrideEditableFlag: Boolean? = null,
        @Query("expand") expand: String? = null,
        @Body body: UpdateCommentBody
    ): Call<Comment>

    @DELETE("issue/{issueIdOrKey}/comment/{id}")
    fun deleteComment(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Path("id") commentId: String
    ): Call<Void>

    @POST("issueLink")
    fun createIssueLink(
        @Body body: CreateIssueLinkBody
    ): Call<Unit>

    @GET("issueLink/{linkId}")
    abstract fun getIssueLink(
        @Path("linkId") linkId: String
    ): Call<IssueLink>

    @DELETE("issueLink/{linkId}")
    abstract fun deleteIssueLink(
        @Path("linkId") linkId: String
    ): Call<Unit>
}

/**
 * Extends retrofit2.Call with generic response handling logic.
 */
private fun <T> Call<T>.executeOrThrow(): T {
    val response = this.execute()
    if (!response.isSuccessful) {
        if (response.code() in 400..499) {
            throw ClientErrorException(response.code(), "Request failed with code ${response.code()}", response.raw())
        }
        throw JiraClientException("Unexpected code ${response.code()}")
    }
    return response.body() ?: throw JiraClientException("Empty response body")
}

class JiraClient(
    private val jiraUrl: String,
    private val email: String,
    private val apiToken: String,
) {
    private val jiraApi: JiraApi
    val httpClient: OkHttpClient

    init {
        httpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(BasicAuthInterceptor(email, apiToken))
                .build()

        val apiBaseUrl = jiraUrl.plus("rest/api/3/")
        val mediaType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        val retrofit =
            Retrofit
                .Builder()
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
        val payload =
            JiraSearchRequest(
                expand = expand,
                fields = fields,
                jql = jql,
                maxResults = maxResults,
                startAt = startAt,
            )

        return jiraApi.searchIssues(payload).executeOrThrow()
    }

    fun getProject(key: String): Project = jiraApi.getProject(key).executeOrThrow()

    fun getIssue(
        key: String,
        includedFields: String = "*all",
        expand: String = "changelog",
    ): IssueBean = jiraApi.getIssue(key, includedFields, expand).executeOrThrow()

    fun editIssue(
        key: String,
        update: EditIssueBody,
    ): Unit {
        return jiraApi.editIssue(key, update).executeOrThrow()
    }

    fun getCurrentUser(
        expand: String? = null
    ): User = jiraApi.getCurrentUser(expand).executeOrThrow()

    fun getUserGroups(
        accountsId: String,
        username: String? = null,
        key: String? = null,
    ): List<GroupName> = jiraApi.getUserGroups(accountsId, username, key).executeOrThrow()

    fun addAttachment(issueIdOrKey: String, file: File): List<Attachment> {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaType())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        return jiraApi.addAttachment(issueIdOrKey, body).executeOrThrow()
    }

    fun deleteAttachment(attachmentId: String) {
        jiraApi.deleteAttachment(attachmentId).executeOrThrow()
    }

    fun openAttachmentStream(attachmentId: String): InputStream {
        val responseBody = jiraApi.downloadAttachment(attachmentId).executeOrThrow()
        return responseBody.byteStream()
    }

    fun addComment(issueIdOrKey: String, body: BodyType): Comment {
        return jiraApi.addComment(
            issueIdOrKey, body = AddCommentBody(body)
        ).executeOrThrow()
    }

    fun addRestrictedComment(issueIdOrKey: String, body: BodyType, visibility: Visibility): Comment {
        return jiraApi.addComment(
            issueIdOrKey, body = AddCommentBody(body, visibility = visibility)
        ).executeOrThrow()
    }

    fun updateComment(issueIdOrKey: String, commentId: String, body: UpdateCommentBody): Comment {
        return jiraApi.updateComment(issueIdOrKey, commentId, body = body).executeOrThrow()
    }

    fun deleteComment(issueIdOrKey: String, commentId: String) {
        jiraApi.deleteComment(issueIdOrKey, commentId).executeOrThrow()
    }

    fun createIssueLink(body: CreateIssueLinkBody) {
        jiraApi.createIssueLink(body).executeOrThrow()
    }

    fun getIssueLink(linkId: String): IssueLink {
        return jiraApi.getIssueLink(linkId).executeOrThrow()
    }

    fun deleteIssueLink(linkId: String) {
        jiraApi.deleteIssueLink(linkId).executeOrThrow()
    }
}
