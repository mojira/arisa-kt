package io.github.mojira.arisa.apiclient.models

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf

class IssueBeanTest: StringSpec({
    "it should deserialize linked issue without comments" {
        val json = loadJsonFile("linked-issue-without-comments.json")
        val issueBean = JSON.decodeFromString<IssueBean>(json)

        issueBean shouldNotBe null
        issueBean.fields.issuelinks.size shouldBe 1
    }

    "it should provide an empty lists for issue without any attachments or comments" {
        val json = loadJsonFile("basic-issue.json")
        val issueBean = JSON.decodeFromString<IssueBean>(json)

        issueBean.fields.attachment.shouldBeTypeOf<ArrayList<AttachmentBean>>()
        issueBean.fields.attachment.shouldBeEmpty()
        issueBean.fields.comment.shouldBeTypeOf<CommentContainer>()
        issueBean.fields.comment?.comments.shouldBeTypeOf<ArrayList<Comment>>()
        issueBean.fields.comment?.comments.shouldBeEmpty()
    }

    "it should deserialize linked issue with attachment and comment" {
        val json = loadJsonFile("linked-issue-with-1-comment-and-2-attachments.json")
        val issueBean = JSON.decodeFromString<IssueBean>(json)

        issueBean shouldNotBe null
        issueBean.fields.comment?.comments?.size shouldBe 1
        issueBean.fields.attachment.size shouldBe 2
    }

    "it should deserialize issue with comments" {
        val json = loadJsonFile("issue-with-3-comments.json")
        val issueBean = JSON.decodeFromString<IssueBean>(json)

        issueBean shouldNotBe null
        issueBean.fields.comment?.comments?.size shouldBe 3
    }

    "it should deserialize issue with security level" {
        val json = loadJsonFile("issue-with-security-level.json")
        val issueBean = JSON.decodeFromString<IssueBean>(json)

        issueBean.fields.security shouldNotBe null
        issueBean.fields.security?.id shouldBe "10033"
    }
})
