package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.mockAttachment
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class AccessTokenRedactorTest : StringSpec({
    "AccessTokenRedactor should return null if nothing to redact" {
        val attachment = mockAttachment(
            mimeType = "text/plain",
            getContent = { "some text".toByteArray() }
        )
        val redactedAttachment = AccessTokenRedactor.redact(attachment)
        redactedAttachment.shouldBeNull()
    }

    "AccessTokenRedactor should redact access token" {
        val attachment = mockAttachment(
            mimeType = "text/plain",
            getContent = {
                // Example JWT token from https://jwt.io/
                ("some text --accessToken eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6" +
                    "IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c more text"
                ).toByteArray()
            }
        )
        val redactedAttachment = AccessTokenRedactor.redact(attachment)
        redactedAttachment!!.attachment shouldBeSameInstanceAs attachment
        redactedAttachment.redactedContent shouldBe "some text --accessToken ###REDACTED### more text"
    }
})
