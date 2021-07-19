package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.IssueSearcher
import io.github.mojira.arisa.domain.Restriction
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.rcarz.jiraclient.Issue

data class MockedComment(
    val visibilityValue: String = "",
    val author: String = "someUser",
    val body: String = "",
    val updateComment: (content: String) -> Unit = { }
)

data class MockedAttachment(
    val authorName: String = "someUser",
    val removeAttachment: () -> Unit = { }
)

data class MockedIssue(
    val comments: List<String> = listOf(),
    val attachments: List<String> = listOf()
)

fun mockRemoveContentCommand(
    issues: Map<String, MockedIssue>,
    comments: Map<String, MockedComment>,
    attachments: Map<String, MockedAttachment>,
    searchIssues: (String, Int) -> Unit = { _, _ -> },
    getIssue: (String) -> Unit = { }
): RemoveContentCommand {
    return RemoveContentCommand(
        issueSearcher = object : IssueSearcher {
            override fun searchIssues(jql: String, maxResults: Int): Either<Throwable, List<String>> {
                return try {
                    searchIssues(jql, maxResults)
                    issues.keys.toList().right()
                } catch (e: Exception) {
                    e.left()
                }
            }
        },
        getIssue = { id ->
            try {
                getIssue(id)
                (id to mockk<Issue>()).right()
            } catch (e: Exception) {
                e.left()
            }
        },
        execute = { it.run() },
        getCommentsFromIssue = { id, _ ->
            issues[id]!!.comments.map { it to mockk() }
        },
        getVisibilityValueOfComment = { (id, _) ->
            comments[id]!!.visibilityValue
        },
        getAuthorOfComment = { (id, _) ->
            comments[id]!!.author
        },
        getBodyOfComment = { (id, _) ->
            comments[id]!!.body
        },
        updateComment = { (id, _), content ->
            comments[id]!!.updateComment(content)
        },
        getAttachmentsFromIssue = { id, _ ->
            issues[id]!!.attachments.map { it to mockk() }
        },
        getAuthorNameFromAttachment = { (id, _) ->
            attachments[id]!!.authorName
        },
        removeAttachment = { (id, _), _ ->
            attachments[id]!!.removeAttachment()
        }
    )
}

class RemoveContentCommandTest : StringSpec({
    "should remove all matching comments" {
        var calledSearch = false
        var comment: String? = null
        var commentRestriction: Restriction? = null

        var removedInnocentComments = 0
        var removedEvilComments = 0
        var removedRestrictedComments = 0

        val innocentComment = MockedComment(
            updateComment = { removedInnocentComments++ }
        )
        val evilComment = MockedComment(
            author = "evilUser",
            updateComment = { removedEvilComments++ }
        )
        val restrictedComment = MockedComment(
            visibilityValue = "staff",
            author = "evilUser",
            updateComment = { removedRestrictedComments++ }
        )

        val comments = mapOf(
            "ic1" to innocentComment,
            "ic2" to innocentComment,
            "ic3" to innocentComment,
            "ic4" to innocentComment,
            "ec1" to evilComment,
            "ec2" to evilComment,
            "ec3" to evilComment,
            "ec4" to evilComment,
            "rc1" to restrictedComment
        )

        var removedInnocentAttachments = 0
        var removedEvilAttachments = 0

        val innocentAttachment = MockedAttachment(
            removeAttachment = { removedInnocentAttachments++ }
        )
        val evilAttachment = MockedAttachment(
            authorName = "evilUser",
            removeAttachment = { removedEvilAttachments++ }
        )

        val attachments = mapOf(
            "ia1" to innocentAttachment,
            "ia2" to innocentAttachment,
            "ea1" to evilAttachment,
            "ea2" to evilAttachment
        )

        val queriedIssues = mutableListOf<String>()

        val issues = mapOf(
            "MC-1" to MockedIssue(listOf("ec1", "ic1", "ec2")),
            "MC-2" to MockedIssue(listOf("ic2", "rc1", "ic3"), listOf("ia1")),
            "MC-3" to MockedIssue(listOf("ec3", "ec4", "ic4"), listOf("ea1", "ia2", "ea2"))
        )

        val command = mockRemoveContentCommand(
            issues,
            comments,
            attachments,
            searchIssues = { _, _ -> calledSearch = true },
            getIssue = { queriedIssues.add(it) }
        )

        val issue = mockIssue(
            addRawComment = { body, restriction ->
                comment = body
                commentRestriction = restriction
            }
        )

        val result = command(issue, "evilUser")

        result shouldBe 1

        calledSearch.shouldBeTrue()
        queriedIssues.shouldContainExactlyInAnyOrder(issues.keys)

        removedInnocentComments shouldBe 0
        removedEvilComments shouldBe 4
        removedRestrictedComments shouldBe 0

        removedInnocentAttachments shouldBe 0
        removedEvilAttachments shouldBe 2

        comment.shouldNotBeNull()
        commentRestriction shouldBe Restriction.STAFF
    }
})
