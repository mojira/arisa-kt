package io.github.mojira.arisa.modules

import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Resolution
import java.time.Instant
import java.util.Date

val CREATED = Date()
val UPDATED = Date(Instant.now().plusSeconds(3).toEpochMilli());
val AWAITINGRESPONSE = "Awaiting Response"

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(null, CREATED, UPDATED)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(mockResolution("Test"), CREATED, UPDATED)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val module = ReopenAwaitingModule()
        val created = Date()
        val updated = Date(Instant.now().plusSeconds(1).toEpochMilli());
        val request = ReopenAwaitingModuleRequest(mockResolution(AWAITINGRESPONSE), created, updated)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})

fun mockResolution(name: String): Resolution {
    val resolution = mockk<Resolution>()
    every { resolution.name } returns name

    return resolution
}
