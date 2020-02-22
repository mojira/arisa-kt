package io.github.mojira.arisa.modules

import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec

class PiracyModuleTest : StringSpec({
    val ISSUE = mockIssue()

    "should return OperationNotNeededModuleResponse where there is no description or environment" {
        val module = PiracyModule()
        val request = PiracyModuleRequest(ISSUE, null, null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse where description and environment are empty" {
        val module = PiracyModule()
        val request = PiracyModuleRequest(ISSUE, "", "")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
