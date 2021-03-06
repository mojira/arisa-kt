package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixCapitalizationCommand {
    operator fun invoke(issue: Issue): Int {
        val capitalizationRegex =
            """(?<=\.\s|^|!\s|\?\s|\n)[A-Z][A-Za-z\-'0-9]*((\s|,\s|;\s|:\s)[A-Z][A-Za-z\-'0-9]*)*(?=\.|$|!|\?|\n)"""
                .toRegex()

        val exceptions = listOf(
            "I",
            "Minecraft",
            "Mojang"
        )

        var newDescription = issue.description!!
        val matchesDescription = capitalizationRegex.findAll(newDescription)
        matchesDescription
            .map { it.groupValues[0] }
            .forEach {
                newDescription = newDescription.replace(it, it.toLowerCase().capitalize())
            }
        exceptions
            .forEach {
                newDescription = newDescription.replace("\\b${it.toLowerCase()}\\b".toRegex(), it)
            }
        if (newDescription == issue.description) {
            throw CommandExceptions.NO_CAPITALIZATION_MATCHES.create()
        }
        issue.updateDescription(newDescription)
        return 1
    }
}
