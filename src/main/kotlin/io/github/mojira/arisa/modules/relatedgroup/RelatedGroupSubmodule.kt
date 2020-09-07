package io.github.mojira.arisa.modules.relatedgroup

import arrow.core.Either
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse

interface RelatedGroupSubmodule {
    operator fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse>
}