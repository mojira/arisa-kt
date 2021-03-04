package io.github.mojira.arisa.domain.new

data class Link(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue
)