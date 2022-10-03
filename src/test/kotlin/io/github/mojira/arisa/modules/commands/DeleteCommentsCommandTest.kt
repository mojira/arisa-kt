package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeleteCommentsCommandTest : StringSpec({
    "should restrict a comment" {
        val command = DeleteCommentsCommand()
        var messageVar = ""
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    restrict = { message ->
                        messageVar = message
                    }
                )
            )
        )

        val result = command(issue, "spammer")
        result shouldBe 1
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(1)
            messageVar shouldBe "Removed by arisa"
        }
    }

    "should restrict multiple comments" {
        val command = DeleteCommentsCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    restrict = { message ->
                        list.add(message + "1")
                    }
                ),
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    restrict = { message ->
                        list.add(message + "2")
                    }
                )
            )
        )

        val result = command(issue, "spammer")
        result shouldBe 2
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2)
            list shouldBe mutableListOf("Removed by arisa1", "Removed by arisa2")
        }
    }

    "should support name with spaces" {
        val command = DeleteCommentsCommand()
        var messageVar = ""
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "very spammy spammer"
                    ),
                    restrict = { message ->
                        messageVar = message
                    }
                )
            )
        )

        val result = command(issue, "very spammy spammer")
        result shouldBe 1
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(1)
            messageVar shouldBe "Removed by arisa"
        }
    }

    "should not restrict comments not by the specified author" {
        val command = DeleteCommentsCommand()
        val list = mutableListOf<String>()
        var issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "not spammer"
                    ),
                    restrict = { message ->
                        list.add("$message not spammer")
                    }
                ),
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    restrict = { message ->
                        list.add("$message spammer")
                    }
                )
            )
        )

        var result = command(issue, "spammer")
        result shouldBe 1
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2)
            list shouldBe mutableListOf("Removed by arisa spammer")
        }
        list.clear()

        issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "not spammer"
                    ),
                    restrict = { message ->
                        list.add("$message not spammer")
                    }
                )
            )
        )

        result = command(issue, "spammer")
        result shouldBe 0
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(1)
            list shouldBe mutableListOf()
        }
    }

    "should not restrict comments already restricted to staff" {
        val command = DeleteCommentsCommand()
        val list = mutableListOf<String>()
        val issue = mockIssue(
            comments = listOf(
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    visibilityType = "group",
                    visibilityValue = "staff",
                    restrict = { message ->
                        list.add("$message restricted (wrong)")
                    }
                ),
                mockComment(
                    author = mockUser(
                        name = "spammer"
                    ),
                    restrict = { message ->
                        list.add("$message (right)")
                    }
                )
            )
        )

        val result = command(issue, "spammer")
        result shouldBe 1
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2)
            list shouldBe mutableListOf("Removed by arisa (right)")
        }
    }
})
