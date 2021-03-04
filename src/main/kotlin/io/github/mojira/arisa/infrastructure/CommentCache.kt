package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

data class CommentCacheCheckError(val message: String)

object CommentCache {
    private var newCommentCache = Cache<MutableSet<String>>()
    private var oldCommentCache = Cache<MutableSet<String>>()

    /**
     * This field controls whether the comment cache is active or not.
     * For tests, this should be disabled.
     * Not quite happy with this but it works.
     */
    var disabled = false

    /**
     * Checks whether the comment has not been posted before
     * @return If check succeeds (comment not a duplicate), a [Unit]. Otherwise a [CommentCacheCheckError].
     */
    fun check(key: String, comment: String): Either<CommentCacheCheckError, Unit> = Either.fx {
        if (disabled) Unit.right().bind()

        if (newCommentCache.get(key)?.contains(comment) == true) {
            CommentCacheCheckError("This comment has already been posted on $key in the last run:\n$comment")
                .left().bind<CommentCacheCheckError>()
        }

        val newPostedComments = oldCommentCache.getOrAdd(key, mutableSetOf())
        if (newPostedComments.contains(comment)) {
            CommentCacheCheckError("This comment has already been posted on $key in this run:\n$comment")
                .left().bind<CommentCacheCheckError>()
        }

        newPostedComments.add(comment)
    }

    /**
     * Clears the old cache and moves the new cache into it.
     */
    fun flush() {
        newCommentCache = oldCommentCache
        oldCommentCache = Cache()
    }
}
