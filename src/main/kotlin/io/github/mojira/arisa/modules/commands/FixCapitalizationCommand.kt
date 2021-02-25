package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

class FixCapitalizationCommand : Command1<String> {
    override operator fun invoke(issue: Issue, arg: String): Int {
        val capitalizationRegex =
                """(?<=\.\s|^|!\s|\?\s)[A-Z][A-Za-z0-9]*((\s|,\s|;\s|:\s)[A-Z][A-Za-z-'0-9]*)*(?=\.|${'$'}|!|\?)"""
                    .toRegex()

        var newDescription = issue.description!!
        val matchesDescription = capitalizationRegex.findAll(newDescription)
        matchesDescription
                .map { it.groupValues[0] }
                .forEach {
                    newDescription = newDescription.replace(it, it.toLowerCase().capitalize())
                }
        if (newDescription == issue.description) {
            throw CommandExceptions.NO_CAPITALIZATION_MATCHES.create(arg)
        }
        issue.updateDescription(newDescription)
        return 1
    }
}
