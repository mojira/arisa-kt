package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec

class ResolveTrashModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when project is not TRASH" {
        val module = ResolveTrashModule()
        val issue = mockIssue(
            project = mockProject(
                key = "MC"
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when ticket when ticket was open" {
        val module = ResolveTrashModule()
        val issue = mockIssue(
            project = mockProject(
                key = "TRASH"
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
    }
})
