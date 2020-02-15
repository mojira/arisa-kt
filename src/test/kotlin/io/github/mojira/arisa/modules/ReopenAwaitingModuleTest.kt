package io.github.mojira.arisa.modules

import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Resolution
import net.rcarz.jiraclient.User
import java.time.Instant
import java.util.Date

val CREATED = Date()
val UPDATED = Date(Instant.now().plusSeconds(3).toEpochMilli())
val AWAITINGRESPONSE = mockResolution("Awaiting Response")
val REPORTER = "Test"
val COMMENTLIST = listOf(mockComment(REPORTER))

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(null, CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(mockResolution("Test"), CREATED, UPDATED, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val module = ReopenAwaitingModule()
        val created = Date()
        val updated = Date(Instant.now().plusSeconds(1).toEpochMilli())
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, created, updated, COMMENTLIST)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, listOf())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val module = ReopenAwaitingModule()
        val comment = mockComment(REPORTER, Date(Instant.now().plusSeconds(8).toEpochMilli()))
        val request = ReopenAwaitingModuleRequest(AWAITINGRESPONSE, CREATED, UPDATED, listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})

fun mockResolution(name: String): Resolution {
    val resolution = mockk<Resolution>()
    every { resolution.name } returns name

    return resolution
}

fun mockComment(reporter: String, updatedDate: Date = Date(Instant.now().plusSeconds(5).toEpochMilli())): Comment {
    val comment = mockk<Comment>()
    val user = mockk<User>()

    every { user.name } returns reporter
    every { comment.author } returns user
    every { comment.updatedDate } returns updatedDate
    every { comment.createdDate } returns Date(Instant.now().plusSeconds(5).toEpochMilli())

    return comment
}
