package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveNonStaffTagsModule(
    private val removalReason: String,
    private val removablePrefixes: List<String>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val removableTags = comments
                .filter(::hasPrefixedTag)
                .filter(::isNotVolunteerRestricted)
                .map { it.restrict.partially1(removeTags(it.body!!)) }

            assertNotEmpty(removableTags).bind()

            removableTags.forEach { it.invoke() }
        }
    }

    private fun hasPrefixedTag(comment: Comment): Boolean {
        removablePrefixes.forEach {
            if (comment.body!!.contains("""${it}_[A-Z_]+""".toRegex())) {
                return true
            }
        }
        return false
    }

    private fun isNotVolunteerRestricted(comment: Comment) =
        comment.visibilityType != "group" ||
                !listOf("staff", "global-moderators", "helper").contains(comment.visibilityValue)

    private fun removeTags(comment: String): String {
        var newComment = comment
        removablePrefixes.forEach { prefix ->
            val regex = """($prefix)(?!_REMOVED)(_[A-Z_]+)""".toRegex()
            newComment = regex.replace(newComment) {
                "Arisa removed prefix '${it.groupValues[1]}' from '${it.groupValues[2]}';" +
                        "removal reason: $removalReason"
            }
        }
        return newComment
    }
}
