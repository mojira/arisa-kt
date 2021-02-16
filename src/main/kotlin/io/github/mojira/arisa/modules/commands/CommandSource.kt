package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue

data class CommandSource(
    val issue: Issue,
    val comment: Comment
)
