package io.github.mojira.arisa.domain.cloud

data class CloudLink(
    val type: String,
    val outwards: Boolean,
    val issue: CloudLinkedIssue,
    val remove: () -> Unit
)
