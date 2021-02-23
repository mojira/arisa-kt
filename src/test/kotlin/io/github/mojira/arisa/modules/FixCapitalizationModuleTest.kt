package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class FixCapitalizationModuleTest : StringSpec({
    val module = FixCapitalizationModule()

    "should return OperationNotNeededModuleResponse when there is no description" {
        val issue = mockIssue()

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the description does not need replacing" {
        val issue = mockIssue(
            description = "testing"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should replace capitalized sentences in description" {
        var hasUpdatedDescription: String? = null

        val issue = mockIssue(
            description = "Testing Except Capitalized.",
            updateDescription = {
                hasUpdatedDescription = it
                Unit.right()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        hasUpdatedDescription shouldBe "Testing except capitalized."
    }
})
