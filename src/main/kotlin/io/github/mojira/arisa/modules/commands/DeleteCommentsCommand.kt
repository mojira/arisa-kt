package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import java.util.concurrent.TimeUnit

class DeleteCommentsCommand {
    @Suppress("MagicNumber")
    operator fun invoke(issue: Issue, name: String): Int {
        val comments = issue.comments
            .filter { it.visibilityValue != "staff" }
            .filter { it.author.name == name }

        Thread {
            comments
                .forEachIndexed { index, it ->
                    it.restrict("Removed by [~arisabot].")
                    if (index % 10 == 0) {
                        TimeUnit.SECONDS.sleep(1)
                    }
                }
        }.start()

        return comments.size
    }
}
