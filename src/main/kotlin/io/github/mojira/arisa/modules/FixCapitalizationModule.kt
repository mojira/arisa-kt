package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class FixCapitalizationModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val capitalizationRegex = """(?<=\.\s|^|!\s|\?\s)[A-Z][A-Za-z]*((\s|,\s)[A-Z][A-Za-z-']*)*(?=\.|${'$'}|!|\?)""".toRegex()

            assertAfter(issue.created, lastRun).bind()

            assertNotNull(description).bind()

            var newDescription = description!!
            val matchesDescription = capitalizationRegex.findAll(newDescription)
            assertNotNull(matchesDescription).bind()
            matchesDescription
                    .map { it.groupValues[0] }
                    .forEach {
                        newDescription = newDescription.replace(it, it.toLowerCase().capitalize())
                    }
            updateDescription(newDescription)
        }
    }
}