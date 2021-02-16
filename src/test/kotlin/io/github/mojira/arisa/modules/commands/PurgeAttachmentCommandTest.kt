package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class PurgeAttachmentCommandTest : StringSpec({
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

        val result = command(issue, 1, Int.MAX_VALUE)

        result shouldBe 2
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

        val result = command(issue, 0, 1)

        result shouldBe 2
        attachment0Removed.shouldBeTrue()
        attachment1Removed.shouldBeTrue()
        attachment2Removed.shouldBeFalse()
    }
})
