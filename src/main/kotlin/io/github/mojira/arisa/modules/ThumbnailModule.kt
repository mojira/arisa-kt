package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.Imaging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant

/** Maximum number of bytes which may be read from an image */
const val IMAGE_MAX_BYTES: Long = 1024 * 5 // 5 KiB

/**
 * Maximum width in pixels an image may have; for larger images a thumbnail
 * should be used, see https://github.com/mojira/arisa-kt/issues/165#issuecomment-660259465
 */
const val MAX_WIDTH = 759

/**
 * Maximum height in pixels an image may have; for larger images a thumbnail
 * should be used.
 *
 * Note that the exact value does not matter since Jira can be scrolled vertically; though
 * the value should not be too large nonetheless.
 */
const val MAX_HEIGHT = 600

class ThumbnailModule(private val maxImagesCount: Int) : Module {

    private val log: Logger = LoggerFactory.getLogger(ThumbnailModule::class.java)

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
     * - At start of line or has space in front
     * - At end of line or has space behind
     * - Starting '!' is not followed by space
     * - Ending '!' is not preceded by space
     * - Does not include a '|', since then it already has display settings and trying to modify them
     *   might break the embedded image (or user already specified a reasonable size)
     *
     * Uses lazy quantifier ("+?") so text containing multiple exclamation marks only matches the
     * shortest substrings, e.g. "!a! !b!" matches "!a!" and "!b!"
     */
    private val imageRegex = """(?<=(?:\s|^)!)(?!\s)[^|]+?(?<!\s)(?=!(?:\s|$))""".toRegex()

    private fun replaceEmbeddedImages(issue: Issue, text: String): String {
        var matchIndex = 0
        return imageRegex.replace(text) {
            val imageName = it.value
            // If replaced maximum number of images, just return imageName unchanged
            if (matchIndex >= maxImagesCount) {
                return@replace imageName
            }
            matchIndex++

            val options = runBlocking { getImageOptions(issue, imageName) }
            if (options != null) "$imageName|$options" else imageName
        }
    }

    private suspend fun getImageOptions(issue: Issue, imageName: String): String? {
        // For now only support embedded attached images (but not embedded with URL)
        val attachment = issue.attachments.find { it.name == imageName }
        if (attachment == null) {
            log.info("Did not find attachment with name '$imageName' for issue ${issue.key}")
            return null
        }
        return withContext(Dispatchers.IO) {
            attachment.openContentStream().use {
                try {
                    /*
                     * Limit number of bytes which may be read to prevent DoS when embedded image is malformed
                     * or maliciously crafted.
                     * Limiting the number of bytes should not be an issue for most image formats since they
                     * contain dimension information at the beginning of the file.
                     */
                    val dimension = Imaging.getImageSize(LimitingInputStream(it, IMAGE_MAX_BYTES), imageName)
                    return@use if (dimension.width > MAX_WIDTH || dimension.height > MAX_HEIGHT) {
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
