package io.github.mojira.arisa.infrastructure.apiclient.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Serializer for [OffsetDateTime] that handles ISO-8601 timestamps with colon-less offset format.
 *
 * Example of supported format: "2025-02-13T12:32:46.327+0100"
 *
 * @throws DateTimeParseException when the input string cannot be parsed
 */
@Serializer(forClass = OffsetDateTime::class)
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    // Jira API v3 returns DateTime with an offsets without colons
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), formatter)
    }
}
