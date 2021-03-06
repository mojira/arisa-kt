package io.github.mojira.arisa.infrastructure.services

import arrow.core.Either
import com.beust.klaxon.Klaxon
import org.apache.http.HttpStatus
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

const val BASE_URL = "https://api.dandelion.eu/datatxt/li/v1/"

data class Response(val detectedLangs: List<LangDetection>)
data class LangDetection(val lang: String, val confidence: Double)

@Suppress("ReturnCount")
fun getLanguage(token: String?, text: String): Either<Error, Map<String, Double>> {
    if (token == null) return Either.left(Error("Dandelion token is undefined"))

    val request = "token=" + URLEncoder.encode(token, "UTF-8") + "&text=" + URLEncoder.encode(text, "UTF-8")
    with(URL(BASE_URL).openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        doOutput = true

        val wr = OutputStreamWriter(outputStream)
        wr.write(request)
        wr.flush()

        if (responseCode != HttpStatus.SC_OK) {
            return Either.left(Error("$responseCode from translation api"))
        }

        val result = Klaxon().parse<Response>(inputStream) ?: return Either.left(Error("Couldn't deserialize response"))

        return Either.right(result.detectedLangs.map { it.lang to it.confidence }
            .toMap())
    }
}
