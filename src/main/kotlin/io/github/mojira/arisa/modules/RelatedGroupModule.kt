package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.relatedgroup.AddTicketsRelatedGroupSubmodule
import io.github.mojira.arisa.modules.relatedgroup.RelatedGroupSubmodule
import java.time.Instant

class RelatedGroupModule(
    private val arisaUsername: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val arisaComments = comments
                .filter(::isStaffRestricted)
                .filter(::userIsArisa)
                .filter(::isProbablyARelatedGroup)
            assertNotEmpty(arisaComments).bind()

            val isError = arisaComments.any {
                !it.body.isNullOrBlank()
                        && it.body.startsWith("ARISAGROUP_ERROR")
            }

            val results = if (arisaComments.size > 1) {
                null
            } else {
                executeSubmodules(arisaComments[0].body!!, issue)
            }

            when {
                isError -> OperationNotNeededModuleResponse.left().bind()
                results == null -> {
                    addRawRestrictedComment("ARISAGROUP_ERROR Detected multiple groups on this ticket. Stopped processing this ticket", "staff")
                }
                results.isLeft() && (results as Either.Left).a is FailedModuleResponse -> {
                    results.bind()
                }
                results.isRight() -> {
                    results.bind()
                }
                else -> OperationNotNeededModuleResponse.left().bind()
            }
        }
    }

    private fun executeSubmodules(comment: String, issue: Issue): Either<ModuleError, ModuleResponse> {
        val strings = comment.split("\\n+".toRegex())
        for (string in strings) {
            if (string.startsWith("ARISAGROUP_")) {
                val split = comment.split("\\s+".toRegex())
                val arguments = split.toTypedArray()
                return when (split[0]) {
                    "ARISAGROUP_ADD_TICKETS" -> AddTicketsRelatedGroupSubmodule(issue, *arguments)
                    else -> OperationNotNeededModuleResponse.left()
                }
            }
        }
    }

    private fun isProbablyARelatedGroup(comment: Comment) =
        !comment.body.isNullOrBlank() &&
                comment.body.startsWith("ARISAGROUP_") &&
                (comment.body.count { it.isWhitespace() } > 0)

    private fun userIsArisa(comment: Comment) =
        comment.author.name == arisaUsername

    private fun isStaffRestricted(comment: Comment) =
        comment.visibilityType == "group" && (comment.visibilityValue == "staff")
}