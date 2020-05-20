package io.github.mojira.arisa.domain

data class CommentOptions(
    val messageKey: String,
    val variable: String? = null,
    val language: String = "en"
)
