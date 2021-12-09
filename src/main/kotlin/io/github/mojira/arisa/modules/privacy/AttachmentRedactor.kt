package io.github.mojira.arisa.modules.privacy

import io.github.mojira.arisa.domain.Attachment

private const val REDACTED_REPLACEMENT = "###REDACTED###"

/**
 * Important: Redactor implementations should be as specific as possible. They irrecoverably remove information
 * from attachments which can only be recovered when the uploader provides the attachment again (in case they
 * still have the original). Redactors should therefore only remove sensitive data when it is definitely not
 * needed, such as access tokens in JVM crash reports. A redactor should for example not blindly remove (what it
 * assumes to be) e-mail addresses from attachments without actually knowing what kind of attachment it is processing.
 */
interface AttachmentRedactor {
    /**
     * Redacts sensitive data from the attachment content.
     *
     * @return
     *      information about the redacted attachment, or `null`
     *      if nothing was redacted
     */
    fun redact(attachment: Attachment): RedactedAttachment?
}

/** Redacts access tokens passed as command line argument, as found in JVM crash reports. */
object AccessTokenRedactor : AttachmentRedactor {
    // Use lookbehind to only redact the token itself
    private val pattern = Regex("""(?<=(^|\s)--accessToken )[a-zA-Z0-9.+/=\-_]+(?=(\s|$))""")

    override fun redact(attachment: Attachment): RedactedAttachment? {
        if (attachment.hasTextContent()) {
            val original = attachment.getTextContent()
            val redacted = original.replace(pattern, REDACTED_REPLACEMENT)
            if (redacted != original) {
                return RedactedAttachment(attachment, redacted)
            }
        }

        return null
    }
}

data class RedactedAttachment(
    /** The original attachment containing sensitive data */
    val attachment: Attachment,
    /** Attachment content with sensitive data redacted */
    val redactedContent: String
)
