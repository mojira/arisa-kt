package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import java.util.concurrent.TimeUnit

class DeleteCommentsCommand : Command<String> {
    @Suppress("MagicNumber")
    override operator fun invoke(issue: Issue, arg: String): Int {
        val comments = issue.comments
            .filter { it.visibilityValue != "staff" }
            .filter { it.author.name == arg }

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
