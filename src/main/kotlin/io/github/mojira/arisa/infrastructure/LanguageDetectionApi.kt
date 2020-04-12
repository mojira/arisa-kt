package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import com.beust.klaxon.Klaxon
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

const val baseUrl = "https://api.dandelion.eu/datatxt/li/v1/"

data class Response(val detectedLangs: List<LangDetection>)
data class LangDetection(val lang: String, val confidence: Double)

fun getLanguage(token: String, text: String): Either<Error, Map<String, Double>> {
    val request = "token=" + URLEncoder.encode(token, "UTF-8") + "&text=" + URLEncoder.encode(text, "UTF-8")
    with(URL(baseUrl).openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        doOutput = true

        val wr = OutputStreamWriter(outputStream)
        wr.write(request)
        wr.flush()

        if (responseCode != 200) {
            return Either.left(Error("$responseCode from translation api"))
        }

        val result = Klaxon().parse<Response>(inputStream) ?: return Either.left(Error("Couldn't deserialize response"))

        return Either.right(result.detectedLangs.map { it.lang to it.confidence }
            .toMap())
    }
}
