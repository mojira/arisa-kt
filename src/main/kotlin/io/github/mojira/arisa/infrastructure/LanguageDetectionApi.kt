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

private data class Response(val detectedLangs: List<LangDetection>)
private data class LangDetection(val lang: String, val confidence: Double)

// See https://dandelion.eu/docs/api/#api-response-error-codes
private data class ErrorResponse(val message: String, val code: String)

private data class QuotaInfo(val unitsLeft: Double, val unitsResetTime: String?)

// See https://dandelion.eu/docs/api/#api-response-headers
private fun getQuotaInfo(connection: HttpURLConnection): QuotaInfo? {
    val unitsLeft = connection.getHeaderField("X-DL-units-left")?.let(String::toDouble)
    val unitsResetTime = connection.getHeaderField("X-DL-units-reset")

    return if (unitsLeft != null) QuotaInfo(unitsLeft, unitsResetTime) else null
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

class LanguageDetectionApi(private val token: String?, private val quotaWarningThreshold: Double?) {
    private var lastQuotaUnits = 0.0

    private fun checkWarnQuota(connection: HttpURLConnection) {
        if (quotaWarningThreshold == null) return

        getQuotaInfo(connection)?.apply {
            val wasAboveThreshold = lastQuotaUnits > quotaWarningThreshold
            if (wasAboveThreshold && unitsLeft <= quotaWarningThreshold) {
                val resetMessage = unitsResetTime?.let { ", reset: $it" } ?: ""
                logger.warn("Quota nearly used up: units left: $unitsLeft$resetMessage")
            }
            lastQuotaUnits = unitsLeft
        }
    }

    @Suppress("ReturnCount", "ThrowsCount")
    fun getLanguage(text: String): Map<String, Double> {
        requireNotNull(token) { "Dandelion token is undefined" }

        val request = "token=" + URLEncoder.encode(token, UTF_8) + "&text=" + URLEncoder.encode(text, UTF_8)
        with(URL(BASE_URL).openConnection() as HttpURLConnection) {
            this.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/51.0.2704.103 Safari/537.36"
            )
            this.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            requestMethod = "POST"
            doOutput = true

            val wr = OutputStreamWriter(outputStream, UTF_8)
            wr.write(request)
            wr.flush()

            checkWarnQuota(this)

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException(getErrorMessage(this))
            }

            val result = inputStream.use {
                Klaxon().parse<Response>(it) ?: throw IOException("Couldn't deserialize response")
            }

            return result.detectedLangs.associate { it.lang to it.confidence }
        }
    }
}
