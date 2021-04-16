package io.github.mojira.arisa.domain

import java.util.function.Supplier

data class LinkedIssue(
    val key: String,
    val status: String?,
    val issue: Supplier<Issue>?
) {
}
