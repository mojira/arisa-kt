package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.Imaging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant

class ThumbnailModule(
    private val maxImageWidth: Int,
    private val maxImageHeight: Int,
    private val maxImageReadBytes: Long,
    private val maxImagesCount: Int
) : Module {

    private val log: Logger = LoggerFactory.getLogger("ThumbnailModule")

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            var performedUpdate = false

            // Only consider new issues
            if (issue.created.isAfter(lastRun)) {
                val oldDescription = issue.description
                if (oldDescription != null) {
                    val newDescription = replaceEmbeddedImages(issue, oldDescription)
                    if (oldDescription != newDescription) {
                        issue.updateDescription(newDescription)
                        performedUpdate = true
                    }
                }
            }

            // Only consider new comments
            val newComments = comments.filter {
                it.created.isAfter(lastRun)
            }

            newComments.forEach {
                val oldBody = it.body!!
                val newBody = replaceEmbeddedImages(issue, oldBody)
                if (oldBody != newBody) {
                    it.update(newBody)
                    performedUpdate = true
                }
            }

            assertTrue(performedUpdate).bind()
        }
    }

    /*
     * Match !<image-name>!
     * (The matched string only consists of the <image-name> part, e.g. "test.png")
     * Only consider if:
     * - At start of line or has space or '!' in front
     * - At end of line or has space or '!' behind
     * - Starting '!' is not followed by space
     * - Ending '!' is not preceded by space
     * - Does not include a '|', since then it already has display settings and trying to modify them
     *   might break the embedded image (or user already specified a reasonable size)
     *
     * Uses lazy quantifier ("+?") so text containing multiple exclamation marks only matches the
     * shortest substrings, e.g. "!a! !b!" matches "!a!" and "!b!"
     */
    private val imageRegex = """(?<=(?:\s|!|^)!)(?!\s)[^|]+?(?<!\s)(?=!(?:\s|!|$))""".toRegex()

    private fun replaceEmbeddedImages(issue: Issue, text: String): String {
        var matchIndex = 0
        return imageRegex.replace(text) {
            val imageName = it.value
            // If replaced maximum number of images, just return imageName unchanged
            if (matchIndex >= maxImagesCount) {
                return@replace imageName
            }
            matchIndex++

            val options = getImageOptions(issue, imageName)
            if (options != null) "$imageName|$options" else imageName
        }
    }

    private fun getImageOptions(issue: Issue, imageName: String): String? {
        // For now only support embedded attached images (but not embedded with URL)
        val attachment = issue.attachments.find { it.name == imageName }
        if (attachment == null) {
            log.info("Did not find attachment with name '$imageName' for issue ${issue.key}")
            return null
        }
        return runBlocking(Dispatchers.IO) {
            attachment.openContentStream().use {
                try {
                    /*
                     * Limit number of bytes which may be read to prevent DoS when embedded image is malformed
                     * or maliciously crafted.
                     * Limiting the number of bytes should not be an issue for most image formats since they
                     * contain dimension information at the beginning of the file.
                     */
                    val dimension = Imaging.getImageSize(LimitingInputStream(it, maxImageReadBytes), imageName)
                    return@use if (dimension.width > maxImageWidth || dimension.height > maxImageHeight) {
                        "thumbnail"
                    } else {
                        null
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // When reading the image fails just pretend it does not need to use thumbnail
                    if (e is IOException || e is ImageReadException) {
                        log.warn("Failed reading image '$imageName' for issue ${issue.key}", e)
                        return@use null
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
