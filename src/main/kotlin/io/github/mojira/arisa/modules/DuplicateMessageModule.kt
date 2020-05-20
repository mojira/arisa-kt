package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue

class DuplicateMessageModule(
    private val message: String,
    private val privateMessage: String?,
    private val resolutionMessages: Map<String, String?>?
) : Module<DuplicateMessageModule.Request> {
    data class Request(
        val links: List<Link<String?, *>>,
        val comments: List<Comment>,
        val addComment: (key: String, filledText: String) -> Either<Throwable, Unit>
    )

    data class LinkedIssue(
        val key: String,
        val security: String?,
        val resolution: String?
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val parents = links
                .filter(::isDuplicatesLink)
            assertNotEmpty(parents).bind()

            val visibleComments = comments
                .filter(::isVisibleComment)
            assertNotAllMentionedBefore(visibleComments, parents).bind()

            val parentSecurity = parents
                .fold(parents[0].security) { parentSecurity, (_, security) ->
                    if (parentSecurity == security) {
                        parentSecurity
                    } else {
                        null
                    }
                }

            val messageKey = if (parentSecurity != null) {
                privateMessage
            } else {
                val parentResolution = parentPairs
                    .fold(parentPairs[0].security) { parentSecurity, (ticket, security) ->
                        ticket.
                        if (parentSecurity == security) {
                            parentSecurity
                        } else {
                            null
                        }
                    }
            }
            val filledText = parents.getFilledText()
            addComment(messageKey, filledText).toFailedModuleEither().bind()
        }
    }

    private fun List<String>.getFilledText() = when (size) {
        1 -> get(0)
        2 -> "${get(0)}* and *${get(1)}"
        else -> "${subList(0, lastIndex).joinToString("*, *")}*, and *${last()}"
    }

    private fun assertNotAllMentionedBefore(comments: List<Comment>, parents: List<Link<*, *>>) =
        assertNotEmpty(parents.filter { link -> comments.none { it.body.contains(link.issue.key) } })

    private fun isVisibleComment(comment: Comment) = comment.visibilityType == null

    private fun isDuplicatesLink(link: Link<*, *>) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}
