package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ListUserActivityCommandTest : StringSpec({
    "should query user activity and post a comment with all tickets" {
        var calledSearch = false
        var comment: String? = null
        var commentRestrictions: String? = null

        val issueList = listOf("MC-1", "MC-12", "MC-1234", "MC-12345")

        val command = ListUserActivityCommand { _, _ ->
            Either.fx {
                calledSearch = true
                issueList
            }
        }

        val issue = mockIssue(
            addRawRestrictedComment = { body, restrictions ->
                comment = body
                commentRestrictions = restrictions
            }
        )

        val result = command(issue, "userName")

        result shouldBe 1
        calledSearch.shouldBeTrue()
        comment.shouldNotBeNull()
        commentRestrictions.shouldBe("staff")

        issueList.forEach {
            comment.shouldContain(it)
        }
    }
})