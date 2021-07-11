package io.github.mojira.arisa.newdomain

data class Link(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue,
    var exists: Boolean = true
)
