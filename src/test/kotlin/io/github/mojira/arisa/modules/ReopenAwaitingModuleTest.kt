package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Resolution
import net.rcarz.jiraclient.User
import java.time.Instant
import java.util.Date

class ReopenAwaitingModuleTest : StringSpec({
    val CREATED = Instant.now()
    val UPDATED = Instant.now().plusSeconds(3)
    val AWAITINGRESPONSE = mockResolution("Awaiting Response")
    val REPORTER = "Test"
    val COMMENTLIST = listOf(mockComment(REPORTER))

    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule { Unit.right() }
        val request = ReopenAwaitingModuleRequest(null, CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule { Unit.right() }
        val request = ReopenAwaitingModuleRequest(mockResolution("Test"), CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val module = ReopenAwaitingModule { Unit.right() }
        val created = Instant.now()
        val updated = Instant.now().plusSeconds(1)
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, created, updated, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val module = ReopenAwaitingModule { Unit.right() }
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, listOf())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val module = ReopenAwaitingModule { Unit.right() }
        val comment = mockComment(REPORTER, Date(Instant.now().plusSeconds(8).toEpochMilli()))
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should grab the last comment" {
        val module = ReopenAwaitingModule { Unit.right() }
        val commentFail = mockComment(REPORTER, Date(Instant.now().plusSeconds(8).toEpochMilli()))
        val commentSuccess = mockComment(reporter = REPORTER)
        val request =
            ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, listOf(commentSuccess, commentFail))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val module = ReopenAwaitingModule { RuntimeException().left() }
        val request =
            ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return ModuleResponse when ticket is reopened" {
        val module = ReopenAwaitingModule { Unit.right() }
        val request =
            ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun mockResolution(name: String): Resolution {
    val resolution = mockk<Resolution>()
    every { resolution.name } returns name

    return resolution
}

private fun mockComment(
    reporter: String,
    updatedDate: Date = Date(Instant.now().plusSeconds(5).toEpochMilli())
): Comment {
    val comment = mockk<Comment>()
    val user = mockk<User>()

    every { user.name } returns reporter
    every { comment.author } returns user
    every { comment.updatedDate } returns updatedDate
    every { comment.createdDate } returns Date(Instant.now().plusSeconds(5).toEpochMilli())

    return comment
}
