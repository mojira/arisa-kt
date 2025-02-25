package io.github.mojira.arisa.apiclient.interceptors

import io.github.mojira.arisa.log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

class LoggingInterceptor(
    private val methods: List<String> = listOf("POST", "PUT")
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (methods.isNotEmpty() && request.method !in methods) {
            return chain.proceed(request)
        }

        // Log the HTTP method and URL.
        log.debug("[HTTP] {} {}", request.method, request.url)

        // If the request has a body, log it.
        request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset: Charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val bodyString = buffer.readString(charset)
            log.debug(bodyString)
        }

        return chain.proceed(request)
    }
}
