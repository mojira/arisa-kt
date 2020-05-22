package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val MODULE = PrivacyModule()
private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val TEN_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

class PrivacyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when the ticket is marked as private" {
        val issue = mockIssue(
            securityLevel = "private"
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the ticket is created before lastRun" {
        val issue = mockIssue(
            created = TEN_SECONDS_AGO,
            description = "foo@example.com"
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment is created before lastRun" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "foo@example.com",
                    created = TEN_SECONDS_AGO
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment is not public" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "foo@example.com",
                    visibilityType = "group",
                    visibilityValue = "helper"
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the attachment is created before lastRun" {
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    created = TEN_SECONDS_AGO,
                    getContent = { "foo@example.com".toByteArray() }
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the attachment is not a text file" {
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    mimeType = "image/png",
                    getContent = { "foo@example.com".toByteArray() }
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the ticket doesn't match the patterns" {
        val issue = mockIssue(
            summary = "Test"
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should mark as private when the summary contains Email" {
        var hasSetPrivate = false

        val issue = mockIssue(
            summary = "foo@example.com",
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe true
    }

    "should mark as private when the environment contains Email" {
        var hasSetPrivate = false

        val issue = mockIssue(
            environment = "foo@example.com",
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe true
    }

    "should mark as private when the description contains Email" {
        var hasSetPrivate = false

        val issue = mockIssue(
            description = "foo@example.com",
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe true
    }

    "should mark as private when the attachment contains Email" {
        var hasSetPrivate = false

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    getContent = { "foo@example.com".toByteArray() }
                )
            ),
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe true
    }

    "should mark as private when the attachment contains session ID" {
        var hasSetPrivate = false

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    getContent = { "(Session ID is token:My1_hnfNSd3nyQ7IbbnGbTS1fgJuM6JkfH2WEKaTTOLPc)".toByteArray() }
                )
            ),
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe true
    }

    "should restrict to helper when the comment contains Email" {
        var hasSetPrivate = false
        var hasRestrictedComment = false

        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "foo@example.com",
                    restrict = { hasRestrictedComment = true; Unit.right() }
                )
            ),
            setPrivate = { hasSetPrivate = true; Unit.right() }
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasSetPrivate shouldBe false
        hasRestrictedComment shouldBe true
    }

    "should return FailedModuleResponse when restricting comment fails" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "foo@example.com",
                    restrict = { RuntimeException().left() }
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when all exceptions" {
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    body = "foo@example.com",
                    restrict = { RuntimeException().left() }
                ),
                mockComment(
                    body = "bar@example.com",
                    restrict = { RuntimeException().left() }
                )
            )
        )

        val result = MODULE(issue, TWO_SECONDS_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})
