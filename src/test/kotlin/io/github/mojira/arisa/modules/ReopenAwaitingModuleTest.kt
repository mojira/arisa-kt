package io.github.mojira.arisa.modules

import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val request = ReopenAwaitingModuleRequest(null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
