package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import com.beust.klaxon.Klaxon
import org.apache.http.HttpStatus
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

private const val BASE_URL = "https://api.dandelion.eu/datatxt/li/v1/"

data class Response(val detectedLangs: List<LangDetection>)
data class LangDetection(val lang: String, val confidence: Double)

@Suppress("ReturnCount")
fun getLanguage(token: String?, text: String): Either<Error, Map<String, Double>> {
    if (token == null) return Either.left(Error("Dandelion token is undefined"))

    val request = "token=" + URLEncoder.encode(token, UTF_8) + "&text=" + URLEncoder.encode(text, UTF_8)
    with(URL(BASE_URL).openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        doOutput = true

        val wr = OutputStreamWriter(outputStream, UTF_8)
        wr.write(request)
        wr.flush()

        if (responseCode != HttpStatus.SC_OK) {
            return Either.left(Error("$responseCode from translation API"))
        }

        val result = Klaxon().parse<Response>(inputStream) ?: return Either.left(Error("Couldn't deserialize response"))

        return Either.right(result.detectedLangs.associate { it.lang to it.confidence })
    }
}
