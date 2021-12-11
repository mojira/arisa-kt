package io.github.mojira.arisa.modules.thumbnail

import arrow.core.right
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.InputStream

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)
private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)

private fun openClassPathInputStream(path: String): InputStream {
    return ThumbnailModuleTest::class.java.getResourceAsStream(path)!!
}

/** PNG image which does not need thumbnail */
private val PNG_SMALL_IMAGE_STREAM: () -> InputStream = {
    openClassPathInputStream("/thumbnail-module/Small.png")
}

/** JPG image which does not need thumbnail */
private val JPG_SMALL_IMAGE_STREAM: () -> InputStream = {
    openClassPathInputStream("/thumbnail-module/Small.jpg")
}

/** PNG image which needs thumbnail */
private val PNG_LARGE_IMAGE_STREAM: () -> InputStream = {
    openClassPathInputStream("/thumbnail-module/Large.png")
}

/** JPG image which needs thumbnail */
private val JPG_LARGE_IMAGE_STREAM: () -> InputStream = {
    openClassPathInputStream("/thumbnail-module/Large.jpg")
}

/** Malformed image */
private val MALFORMED_IMAGE_STREAM: () -> InputStream = {
    ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0))
}

class ThumbnailModuleTest : StringSpec({
    val maxImagesCount = 2
    val module = ThumbnailModule(
        maxImageWidth = 759,
        maxImageHeight = 600,
        maxImageReadBytes = 1024 * 5, // 5 KiB
        maxImagesCount
    )

    "should return OperationNotNeededModuleResponse when there is no description or comment" {
        val issue = mockIssue()

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue is created before last run" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            created = TWO_SECONDS_AGO,
            description = "!$attachmentName!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment is created before last run" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "!$attachmentName!",
                    created = TWO_SECONDS_AGO
                )
            ),
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when embedded image has settings ('|')" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName|some-option=1!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    // Note: Embedded images with URL reference are not supported by module
    "should return OperationNotNeededModuleResponse when embedded image is not attachment" {
        val issue = mockIssue(
            description = "!https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png!"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when embedded image does not exist" {
        val issue = mockIssue(
            description = "!does-not-exist.png!"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when embedded PNG image does not need thumbnail" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_SMALL_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when embedded JPG image does not need thumbnail" {
        val attachmentName = "test.jpg"
        val issue = mockIssue(
            description = "!$attachmentName!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = JPG_SMALL_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when embedded image attachment is malformed" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = MALFORMED_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for malformed / ambiguous embedded image references" {
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!$attachmentName! !|$attachmentName!",
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should use PNG thumbnail for description" {
        var hasUpdatedDescription: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName|thumbnail!"
    }

    "should use JPG thumbnail for description" {
        var hasUpdatedDescription: String? = null
        val attachmentName = "test.jpg"
        val issue = mockIssue(
            description = "!$attachmentName!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = JPG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName|thumbnail!"
    }

    "should use thumbnail for comments" {
        var hasUpdatedComment1: String? = null
        var hasUpdatedComment2: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "!$attachmentName!",
                    update = {
                        hasUpdatedComment1 = it
                        Unit.right()
                    }
                ),
                mockComment(
                    body = "Hello !$attachmentName!",
                    update = {
                        hasUpdatedComment2 = it
                        Unit.right()
                    }
                )
            ),
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedComment1 shouldBe "!$attachmentName|thumbnail!"
        hasUpdatedComment2 shouldBe "Hello !$attachmentName|thumbnail!"
    }

    "should use thumbnail for description and comment" {
        var hasUpdatedDescription: String? = null
        var hasUpdatedComment: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            comments = listOf(
                mockComment(
                    body = "!$attachmentName!",
                    update = {
                        hasUpdatedComment = it
                        Unit.right()
                    }
                )
            ),
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName|thumbnail!"
        hasUpdatedComment shouldBe "!$attachmentName|thumbnail!"
    }

    "should use thumbnail for description multiple" {
        var hasUpdatedDescription: String? = null
        val attachmentName1 = "test1.png"
        val attachmentName2 = "test2.png"
        val issue = mockIssue(
            description = "!$attachmentName1!\nAnd another !$attachmentName2!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName1,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                ),
                mockAttachment(
                    name = attachmentName2,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName1|thumbnail!\nAnd another !$attachmentName2|thumbnail!"
    }

    "should use thumbnail for description with maxImagesCount limit" {
        var hasUpdatedDescription: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName! ".repeat(maxImagesCount + 2),
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        // Module is created with maxImagesCount=2, should only process at most two images
        hasUpdatedDescription shouldBe ("!$attachmentName|thumbnail! !$attachmentName|thumbnail! " +
            "!$attachmentName! !$attachmentName! ")
    }

    "regex should match embedded images lazily" {
        var hasUpdatedDescription: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            // Regex should match only "!$attachmentName!"
            description = "!$attachmentName! and more text!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName|thumbnail! and more text!"
    }

    "regex should match multiple images without space between" {
        var hasUpdatedDescription: String? = null
        val attachmentName = "test.png"
        val issue = mockIssue(
            description = "!$attachmentName!!$attachmentName!",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            },
            attachments = listOf(
                mockAttachment(
                    name = attachmentName,
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "!$attachmentName|thumbnail!!$attachmentName|thumbnail!"
    }

    "regex should not match newlines and exclamation marks as the file name" {
        val issue = mockIssue(
            // from MCPE-120943
            description = "Hello !!!\n" +
                "I’ve found the ultimate solution to the problem !!!!! \n" +
                "Just login from settings and not the main menu ! " +
                "Enter the code given to enter in aka.ms/remoteconnect and all done !!!!",
            attachments = listOf(
                mockAttachment(
                    name = "!",
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                ),
                mockAttachment(
                    name = "!\nI’ve found the ultimate solution to the problem !",
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                ),
                mockAttachment(
                    name = "! \nJust login from settings and not the main menu ! " +
                        "Enter the code given to enter in aka.ms/remoteconnect and all done !",
                    openInputStream = PNG_LARGE_IMAGE_STREAM
                )
            )
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
