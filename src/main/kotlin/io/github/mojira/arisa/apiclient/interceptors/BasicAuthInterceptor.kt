package io.github.mojira.arisa.apiclient.interceptors

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

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
        val newRequest: Request =
            originalRequest
                .newBuilder()
                .header("Authorization", credentials)
                .build()

        return chain.proceed(newRequest)
    }
}
