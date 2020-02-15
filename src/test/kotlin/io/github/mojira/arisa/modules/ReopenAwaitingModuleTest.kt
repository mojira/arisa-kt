package io.github.mojira.arisa.modules

import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Resolution

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(mockResolution("Test"))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

})

fun mockResolution(name: String): Resolution {
    val resolution = mockk<Resolution>()
    every { resolution.name } returns name

    return resolution
}
