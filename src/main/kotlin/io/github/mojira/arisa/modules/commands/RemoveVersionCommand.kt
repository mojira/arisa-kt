package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertTrue

class RemoveVersionCommand : Command {
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 1).bind()
        val version = arguments.asList().subList(1, arguments.size).joinToString(" ")
        assertTrue(issue.affectedVersions.any { it.name == version }).bind()
        assertTrue(issue.project.versions.any { it.name == version }).bind()
        assertNotNull(issue.affectedVersions.indexOf(issue.project.versions.first { it.name == version })).bind()
        val idNum = issue.affectedVersions.indexOf(issue.project.versions.first { it.name == version })
        issue.affectedVersions[idNum].remove
    }
}
