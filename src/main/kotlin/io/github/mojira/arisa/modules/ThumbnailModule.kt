package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

const val TMB_WIDTH = 200
const val TMB_HEIGHT = 145

class ThumbnailModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val commentsWithImages = comments.filter{
                it.created.isAfter(lastRun)
            }.filter{
                it.body?.contains(imageRegex) ?: false
            }
            assertNotEmpty(commentsWithImages).bind()
        }
    }

    private val imageRegex = "[\\s^]!.+?![\\s$]".toRegex()

    private fun String.getNextImageMatch(): MatchResult? {
        val match = imageRegex.find(this)
        // weird quirk of jira, "!test!|test!" is not an embed, but "!test!test!" is
        return if (match != null && match.value.last() == '|')
            match.getNextImageMatch()
        else
            match
    }

    private fun MatchResult.getNextImageMatch(): MatchResult? {
        var match: MatchResult? = this
        do {
            match = match?.next()
        // weird quirk of jira, "!test!|test!" is not an embed, but "!test!test!" is
        } while (match != null && match.value.last() == '|')
        return match
    }
}