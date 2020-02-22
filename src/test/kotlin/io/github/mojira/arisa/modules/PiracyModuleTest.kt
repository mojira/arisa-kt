package io.github.mojira.arisa.modules

import arrow.core.right
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.specs.StringSpec

class PiracyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse where there is no description, summary or environment" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() })
        val request = PiracyModuleRequest(null, null, null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse where description, summary and environment are empty" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() })
        val request = PiracyModuleRequest("", "", "")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if description contains a piracy signature" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() })
        val request = PiracyModuleRequest("", "", "")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

})
