package io.github.mojira.arisa.infrastructure

import com.beust.klaxon.Klaxon
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

private const val BASE_URL = "https://api.dandelion.eu/datatxt/li/v1/"

private val logger = LoggerFactory.getLogger("LanguageDetectionApi")

data class Response(val detectedLangs: List<LangDetection>)
data class LangDetection(val lang: String, val confidence: Double)

// See https://dandelion.eu/docs/api/#api-response-error-codes
data class ErrorResponse(val message: String, val code: String)

// See https://dandelion.eu/docs/api/#api-response-headers
private fun getQuotaInfo(connection: HttpURLConnection): String? {
    val unitsLeft = connection.getHeaderField("X-DL-units-left")
    val unitsReset = connection.getHeaderField("X-DL-units-reset")

    val stringBuilder = StringBuilder()
    if (unitsLeft != null) {
        stringBuilder.append("units left: ").append(unitsLeft)
    }
    if (unitsReset != null) {
        if (stringBuilder.isNotEmpty()) {
            stringBuilder.append(", ")
        }
        stringBuilder.append("units reset: ").append(unitsReset)
    }
    return stringBuilder.toString().takeIf(String::isNotEmpty)
}

private fun getErrorMessage(connection: HttpURLConnection): String {
    var detailedErrorMessage = ""
    try {
        val stream = connection.errorStream ?: connection.inputStream
        val errorResponse =
            stream.use { Klaxon().parse<ErrorResponse>(it) }

        if (errorResponse != null) {
            detailedErrorMessage = ": code=${errorResponse.code}, message=${errorResponse.message}"
        }
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        logger.debug("Ignoring exception while parsing language API error response", exception)
    }

    return "${connection.responseCode} from language API$detailedErrorMessage"
}

@Suppress("ReturnCount", "ThrowsCount")
fun getLanguage(token: String?, text: String): Map<String, Double> {
    if (token == null) throw IllegalArgumentException("Dandelion token is undefined")

    val request = "token=" + URLEncoder.encode(token, UTF_8) + "&text=" + URLEncoder.encode(text, UTF_8)
    with(URL(BASE_URL).openConnection() as HttpURLConnection) {
        this.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/51.0.2704.103 Safari/537.36"
        )
        this.setRequestProperty("Content-Type", "application/json")
        requestMethod = "POST"
        doOutput = true

        val wr = OutputStreamWriter(outputStream, UTF_8)
        wr.write(request)
        wr.flush()

        getQuotaInfo(this)?.let {
            logger.info("Quota: $it")
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException(getErrorMessage(this))
        }

        val result = inputStream.use {
            Klaxon().parse<Response>(it) ?: throw IOException("Couldn't deserialize response")
        }

        return result.detectedLangs.associate { it.lang to it.confidence }
    }
}
