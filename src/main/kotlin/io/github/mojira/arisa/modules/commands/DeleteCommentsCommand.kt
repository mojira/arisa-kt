package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue
import java.util.concurrent.TimeUnit

/**
 * After how many actions the bot should pause for a second
 * (in order to not send too many requests too quickly)
 */
const val DELETE_COMMENT_SLEEP_INTERVAL = 10

@Suppress("ExplicitItLambdaParameter")
class DeleteCommentsCommand {
    operator fun invoke(issue: Issue, userName: String): Int {
        val comments = issue.comments
            .filter { it.visibilityValue != "staff" }
            .filter { it.author.name == userName }

        Thread {
            comments
                .forEachIndexed { index, it ->
                    it.restrict("Removed by arisa")
                    if (index % DELETE_COMMENT_SLEEP_INTERVAL == 0) {
                        TimeUnit.SECONDS.sleep(1)
                    }
                }
        }.start()

        return comments.size
    }
}
