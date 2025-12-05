package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.cloud.CloudIssue

data class CommandSource(
    val issue: CloudIssue,
    val comment: Comment,
    val line: Int
)
