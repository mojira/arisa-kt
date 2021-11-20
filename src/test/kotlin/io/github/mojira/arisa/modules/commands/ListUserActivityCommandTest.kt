package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

class ListUserActivityCommandTest : StringSpec({
    "should query user activity and post a comment with all tickets" {
        var calledSearch = false
        var comment: String? = null
        var commentRestriction: String? = null

        val issueList = listOf("MC-1", "MC-12", "MC-1234", "MC-12345")

        val command = ListUserActivityCommand { _, _ ->
            Either.fx {
                calledSearch = true
                issueList
            }
        }

        val issue = mockIssue(
            addRawRestrictedComment = { body, restriction ->
                comment = body
                commentRestriction = restriction
            }
        )

        val result = command(issue, "user\nName")

        result shouldBe 1
        calledSearch.shouldBeTrue()
        comment.shouldNotBeNull()
        commentRestriction shouldBe "staff"

        issueList.forEach {
            comment shouldContain it
        }
        // Should contain sanitized user name
        comment shouldContain "user?Name"
    }

    "should post comment when no tickets were found" {
        var calledSearch = false
        var comment: String? = null
        var commentRestriction: String? = null

        val command = ListUserActivityCommand { _, _ ->
            Either.fx {
                calledSearch = true
                emptyList()
            }
        }

        val issue = mockIssue(
            addRawRestrictedComment = { body, restriction ->
                comment = body
                commentRestriction = restriction
            }
        )

        val result = command(issue, "user\nName")

        result shouldBe 1
        calledSearch.shouldBeTrue()
        comment.shouldNotBeNull()
        commentRestriction shouldBe "staff"

        // Should contain sanitized user name
        comment shouldStartWith "No unrestricted comments from user \"user?Name\" were found."
    }
})
