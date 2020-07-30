package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class PurgeAttachmentCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are too many arguments" {
        val command = PurgeAttachmentCommand()

        val issue = mockIssue()

        val result = command(issue, "ARISA_PURGE_ATTACHMENT", "0", "1", "2")

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should purge all attachments when no ID is specified" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true }
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true }
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true }
                )
            )
        )

        val result = command(issue, "ARISA_PURGE_ATTACHMENT")

        result.shouldBeRight(ModuleResponse)
        attachment0Removed.shouldBeTrue()
        attachment1Removed.shouldBeTrue()
        attachment2Removed.shouldBeTrue()
    }

    "should purge all attachments after 1" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true }
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true }
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true }
                )
            )
        )

        val result = command(issue, "ARISA_PURGE_ATTACHMENT", "1")

        result.shouldBeRight(ModuleResponse)
        attachment0Removed.shouldBeFalse()
        attachment1Removed.shouldBeTrue()
        attachment2Removed.shouldBeTrue()
    }

    "should purge all attachments between 0 and 1" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true }
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true }
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true }
                )
            )
        )

        val result = command(issue, "ARISA_PURGE_ATTACHMENT", "0", "1")

        result.shouldBeRight(ModuleResponse)
        attachment0Removed.shouldBeTrue()
        attachment1Removed.shouldBeTrue()
        attachment2Removed.shouldBeFalse()
    }

    "should return FailedModuleResponse when the argument is illegal" {
        val command = PurgeAttachmentCommand()

        val issue = mockIssue()

        val result = command(issue, "ARISA_PURGE_ATTACHMENT", "illegal")

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
