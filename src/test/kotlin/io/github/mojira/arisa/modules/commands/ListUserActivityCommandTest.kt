package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.IssueSearcher
import io.github.mojira.arisa.domain.Restriction
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
        var commentRestriction: Restriction? = null

        val issueList = listOf("MC-1", "MC-12", "MC-1234", "MC-12345")

        val command = ListUserActivityCommand(
            object : IssueSearcher {
                override fun searchIssues(jql: String, maxResults: Int): Either<Throwable, List<String>> {
                    return Either.fx {
                        calledSearch = true
                        issueList
                    }
                }
            }
        )

        val issue = mockIssue(
            addRawComment = { body, restriction ->
                comment = body
                commentRestriction = restriction
            }
        )

        val result = command(issue, "userName")

        result shouldBe 1
        calledSearch.shouldBeTrue()
        comment.shouldNotBeNull()
        commentRestriction shouldBe Restriction.STAFF

        issueList.forEach {
            comment.shouldContain(it)
        }
    }
})
