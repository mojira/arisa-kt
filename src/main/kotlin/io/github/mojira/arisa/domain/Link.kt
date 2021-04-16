package io.github.mojira.arisa.domain

data class Link(
    val id: String?,
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue?
)