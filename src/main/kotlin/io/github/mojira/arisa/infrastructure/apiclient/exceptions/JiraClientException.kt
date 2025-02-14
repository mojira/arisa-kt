package io.github.mojira.arisa.infrastructure.apiclient.exceptions

import io.github.mojira.arisa.infrastructure.apiclient.JiraClient
import okhttp3.Response

/**
 * Base exception for [JiraClient].
 */
open class JiraClientException(
    message: String,
) : Exception(message)

/**
 * An exception signaling client error response (status [code] 400-499).
 */
open class ClientErrorException(
    val code: Int,
    val result: String,
    val response: Response,
) : JiraClientException(result)
