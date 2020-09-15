package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.relatedgroup.RelatedGroupSubmodule
import io.github.mojira.arisa.modules.relatedgroup.AddTicketsRelatedGroupSubmodule
import java.time.Instant

class RelatedGroupModule(
    private val arisaUsername: String,
    val addTicketsRelatedGroupSubmodule: RelatedGroupSubmodule = AddTicketsRelatedGroupSubmodule()
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

            var results: List<Either<ModuleError,ModuleResponse>> = listOf()
            if (arisaComments.size == 1) {
                val strings = arisaComments[0].body!!.split("\\n+".toRegex())
                results = strings.filter {
                    it.startsWith("ARISAGROUP_")
                }.map {
                    executeSubmodule(it, issue)
                }
            }

            when {
                isError -> OperationNotNeededModuleResponse.left().bind()
                results.isEmpty() -> {
                    addRawRestrictedComment("ARISAGROUP_ERROR Detected multiple groups on this ticket. Stopped processing this ticket", "staff")
                }
                results.any { it.isLeft() && (it as Either.Left).a is FailedModuleResponse } -> {
                    results.first { (it as Either.Left).a is FailedModuleResponse }.bind()
                }
                results.any { it.isRight() } -> {
                    results.first { it.isRight() }.bind()
                }
                else -> OperationNotNeededModuleResponse.left().bind()
            }
        }
    }

    private fun executeSubmodule(comment: String, issue: Issue): Either<ModuleError, ModuleResponse> {
        val split = comment.split("\\s+".toRegex())
        val arguments = split.toTypedArray()
        return when (split[0]) {
            "ARISAGROUP_ADD_TICKETS" -> addTicketsRelatedGroupSubmodule(issue, *arguments)
            else -> OperationNotNeededModuleResponse.left()
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