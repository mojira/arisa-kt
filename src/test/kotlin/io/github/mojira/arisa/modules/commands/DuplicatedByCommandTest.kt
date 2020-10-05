package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.core.spec.style.StringSpec

class DuplicatedByCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no argument is passed" {
        val command = DuplicatedByCommand()

        val result = command(mockIssue(), "ARISA_DUPLICATED_BY")
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse in Either when given not a ticket" {
        val command = DuplicatedByCommand()

        val result = command(mockIssue(), "ARISA_ADD_LINKS", "relates", "MC-1", "https://bugs.mojang.com/browse/MC-2", "lalala", "MC-3")
        result shouldBeLeft OperationNotNeededModuleResponse
    }
})