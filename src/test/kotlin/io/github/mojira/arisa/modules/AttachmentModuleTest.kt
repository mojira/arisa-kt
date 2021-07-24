package io.github.mojira.arisa.modules

import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant

private val NOW = Instant.now()

class AttachmentModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no attachments" {
        val module = AttachmentModule(emptyList(), "attach-new-attachment")
        val issue = mockIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no blacklisted attachments" {
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            name = "testfile"
        )
        val issue = mockIssue(
            attachments = listOf(attachment)
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when something is deleted successfully" {
        var removedAttachment = false
        var observedCommentOptions = CommentOptions("")
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            remove = { removedAttachment = true }
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            addComment = { observedCommentOptions = it }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        removedAttachment.shouldBeTrue()
        observedCommentOptions shouldBe CommentOptions("attach-new-attachment")
    }

    "should comment with attachment details when an attachment is removed" {
        var removedAttachment = false
        var comment = ""
        var commentRestriction: String? = null
        val module = AttachmentModule(listOf(".test"), "attach-new-attachment")
        val attachment = getAttachment(
            name = "evil\nAttachment.test",
            uploaderName = "evil\nUser",
            remove = { removedAttachment = true }
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            addRawRestrictedComment = { body, restriction ->
                comment = body
                commentRestriction = restriction
            }
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
        removedAttachment.shouldBeTrue()
        commentRestriction shouldBe "helper"
        // Should contain sanitized user name
        comment shouldContain "evil?User"
        // Should contain sanitized attachment name
        comment shouldContain "evil?Attachment"
    }
})

private fun getAttachment(
    name: String = "testfile.test",
    created: Instant = NOW,
    uploaderName: String = "someUser",
    remove: () -> Unit = { }
) = mockAttachment(
    name = name,
    created = created,
    uploader = mockUser(name = uploaderName),
    remove = remove,
    getContent = { ByteArray(0) }
)
