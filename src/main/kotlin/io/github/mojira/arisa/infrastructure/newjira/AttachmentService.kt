package io.github.mojira.arisa.infrastructure.newjira

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.rcarz.jiraclient.JiraClient
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import java.io.IOException
import java.io.InputStream

class AttachmentService(private val jiraClient: JiraClient) {
    fun openContentStream(id: String, contentUrl: String): InputStream {
        val httpClient = jiraClient.restClient.httpClient
        val request = HttpGet(contentUrl)

        return runBlocking(Dispatchers.IO) {
            val response = httpClient.execute(request)
            val statusCode = response.statusLine.statusCode
            if (statusCode != HttpStatus.SC_OK) {
                throw IOException("Request for attachment $id content failed with status code $statusCode")
            }
            response.entity.content
        }
    }
}
