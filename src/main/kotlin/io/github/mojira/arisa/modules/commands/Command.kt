package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse

interface Command {
    operator fun invoke(commandInfo: CommandInfo, issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse>
}
