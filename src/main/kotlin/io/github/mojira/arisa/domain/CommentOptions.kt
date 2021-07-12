package io.github.mojira.arisa.domain

data class CommentOptions(
    val messageKey: String,
    val variable: String? = null,
    val signatureMessageKey: String? = null,
    val language: String = "en",
    val restriction: Restriction? = null
)

/** Represents a group visibility restriction. */
enum class Restriction(
    /** Group name used by Jira */
    val groupName: String
) {
    HELPER("helper"),
    STAFF("staff")
}
