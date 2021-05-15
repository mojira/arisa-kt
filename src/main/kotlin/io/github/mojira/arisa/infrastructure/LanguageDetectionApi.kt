package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import com.beust.klaxon.Klaxon
import io.github.mojira.arisa.modules.openHttpPostInputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

const val BASE_URL = "https://api.dandelion.eu/datatxt/li/v1/"

data class Response(val detectedLangs: List<LangDetection>)
data class LangDetection(val lang: String, val confidence: Double)

@Suppress("ReturnCount")
fun getLanguage(token: String?, text: String): Either<Error, Map<String, Double>> {
    if (token == null) return Either.left(Error("Dandelion token is undefined"))

    val request = "token=" + URLEncoder.encode(token, UTF_8) + "&text=" + URLEncoder.encode(text, UTF_8)
    openHttpPostInputStream(URI(BASE_URL), request).use { inputStream ->
        val result = Klaxon().parse<Response>(inputStream)
            ?: return Either.left(Error("Couldn't deserialize response"))

        return Either.right(result.detectedLangs.associate { it.lang to it.confidence })
    }
}
