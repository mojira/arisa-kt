package io.github.mojira.arisa.modules

import arrow.core.left
import io.github.mojira.arisa.utils.NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class ResolveTrashModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when project is not TRASH" {
        val module = ResolveTrashModule()
        val issue = mockIssue(
            project = mockProject(
                key = "MC"
            )
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when ticket when ticket was open" {
        val module = ResolveTrashModule()
        val issue = mockIssue(
            project = mockProject(
                key = "TRASH"
            )
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = ResolveTrashModule()
        val issue = mockIssue(
            project = mockProject(
                key = "TRASH"
            ),
            resolveAsInvalid = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
