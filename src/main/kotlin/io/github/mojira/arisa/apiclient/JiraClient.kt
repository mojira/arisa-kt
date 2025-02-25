@file:Suppress("ktlint")

package io.github.mojira.arisa.apiclient

import io.github.mojira.arisa.apiclient.exceptions.JiraClientException
import io.github.mojira.arisa.apiclient.exceptions.ClientErrorException
import io.github.mojira.arisa.apiclient.interceptors.BasicAuthInterceptor
import io.github.mojira.arisa.apiclient.interceptors.LoggingInterceptor
import io.github.mojira.arisa.apiclient.models.IssueBean
import io.github.mojira.arisa.apiclient.models.Project
import io.github.mojira.arisa.apiclient.models.SearchResults
import io.github.mojira.arisa.apiclient.models.AttachmentBean
import io.github.mojira.arisa.apiclient.models.BodyType
import io.github.mojira.arisa.apiclient.models.Comment
import io.github.mojira.arisa.apiclient.models.GroupName
import io.github.mojira.arisa.apiclient.models.IssueLink
import io.github.mojira.arisa.apiclient.models.User
import io.github.mojira.arisa.apiclient.models.Visibility
import io.github.mojira.arisa.apiclient.requestModels.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.MultipartBody
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
import retrofit2.http.QueryMap
import java.io.File
import java.io.InputStream

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
    ): Call<List<AttachmentBean>>

    @DELETE("attachment/{id}")
    fun deleteAttachment(
        @Path("id") attachmentId: String
    ): Call<Unit>

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
        @QueryMap options: Map<String, String>,
        @Body body: UpdateCommentBody
    ): Call<Comment>

    @DELETE("issue/{issueIdOrKey}/comment/{id}")
    fun deleteComment(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Path("id") commentId: String
    ): Call<Unit>

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

    @GET("issue/{issueIdOrKey}/transitions")
    fun getTransition(
        @Path("issueIdOrKey") issueIdOrKey: String,
    ): Call<Unit>

    @POST("issue/{issueIdOrKey}/transitions")
    fun performTransition(
        @Path("issueIdOrKey") issueIdOrKey: String,
        @Body body: TransitionIssueBody
    ): Call<Unit>
}

/**
 * Extends retrofit2.Call with generic response handling logic.
 */
private inline fun <reified T> Call<T>.executeOrThrow(): T {
    val response = this.execute()
    val originalRequest = response.raw().request

    if (!response.isSuccessful) {
        if (response.code() in 400..499) {
            throw ClientErrorException(
                response.code(),
                "Request failed - [${response.code()}] ${originalRequest.method} ${originalRequest.url}",
                response.raw()
            )
        }
        throw JiraClientException("Unexpected code ${response.code()}")
    }

    if (T::class == Unit::class) {
        return Unit as T
    }

    val body = response.body()
    if (body != null) {
        return body
    }

    throw JiraClientException("Empty response body")
}

class JiraClient(
    private val jiraUrl: String,
    private val email: String,
    private val apiToken: String,
    private val logHttpRequests: Boolean?
) {
    private val jiraApi: JiraApi
    val httpClient: OkHttpClient

    init {
        httpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(BasicAuthInterceptor(email, apiToken))
                .apply {
                    if (logHttpRequests == true) {
                        addInterceptor(LoggingInterceptor())
                    }
                }
                .build()

        val apiBaseUrl = jiraUrl.plus("rest/api/2/")
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

    fun addAttachment(issueIdOrKey: String, file: File): List<AttachmentBean> {
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

    /**
     * Downloads the attachment content as a byte array.
     * The content refers to [AttachmentBean.content] which a direct URL for given attachment.
     */
    fun downloadAttachment(attachmentId: String): ByteArray {
        val responseBody = jiraApi.downloadAttachment(attachmentId).executeOrThrow()
        return responseBody.bytes()
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

    fun updateComment(
        issueIdOrKey: String,
        commentId: String,
        body: UpdateCommentBody,
        updateCommentQueryParams: UpdateCommentQueryParams = UpdateCommentQueryParams()
    ): Comment {
        return jiraApi.updateComment(issueIdOrKey, commentId, options = updateCommentQueryParams.toMap(), body = body)
            .executeOrThrow()
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

    fun getTransition(issueIdOrKey: String) {
        jiraApi.deleteIssueLink(issueIdOrKey).executeOrThrow()
    }

    fun performTransition(issueIdOrKey: String, body: TransitionIssueBody) {
        jiraApi.performTransition(issueIdOrKey, body).executeOrThrow()
    }
}
