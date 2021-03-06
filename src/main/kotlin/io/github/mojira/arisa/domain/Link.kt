package io.github.mojira.arisa.domain

data class Link(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue
)