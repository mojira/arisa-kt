package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse
import org.apache.http.HttpEntity

@Serializable
data class AttachmentBean(
    @SerialName("id")
    val id: String,

    @SerialName("filename")
    val filename: String,

    @SerialName("created")
    val created: String,

    @SerialName("mimeType")
    val mimeType: String,

    @SerialName("content")
    val content: String,

    @SerialName("author")
    val author: UserDetails? = null,

    @SerialName("size")
    val size: Int,
)

@Throws(Exception::class)
fun AttachmentBean.download(): ByteArray {
    val bos = ByteArrayOutputStream()
    val httpClient: CloseableHttpClient = HttpClients.createDefault()

    try {
        val get = HttpGet(this.content)
        val response: HttpResponse = httpClient.execute(get)
        val entity: HttpEntity? = response.entity
        entity?.content?.use { inputStream ->
            var next = inputStream.read()
            while (next > -1) {
                bos.write(next)
                next = inputStream.read()
            }
            bos.flush()
        }
    } catch (e: IOException) {
        throw Exception("Failed downloading attachment: ${e.message}")
    } finally {
        httpClient.close()
    }

    return bos.toByteArray()
}
