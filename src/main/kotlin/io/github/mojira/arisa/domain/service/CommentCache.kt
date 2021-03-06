package io.github.mojira.arisa.domain.service

import io.github.mojira.arisa.infrastructure.services.Cache

data class CommentCacheCheckError(val message: String)

class CommentCache {
    private var newCommentCache = Cache<MutableSet<String>>()
    private var oldCommentCache = Cache<MutableSet<String>>()

    /**
     * Checks whether the comment has not been posted before
     * @return If check succeeds (comment not a duplicate), a [Unit]. Otherwise a [CommentCacheCheckError].
     */
    fun hasBeenPostedBefore(key: String, comment: String): Boolean = when {
        newCommentCache[key]?.contains(comment) == true -> true
        oldCommentCache[key]?.contains(comment) == true -> true
        else -> false
    }

    fun addPost(key: String, comment: String) {
        newCommentCache.getOrAdd(key, mutableSetOf()).add(comment)
    }

    /**
     * Clears the old cache and moves the new cache into it.
     */
    fun flush() {
        newCommentCache = oldCommentCache
        oldCommentCache = Cache()
    }
}
