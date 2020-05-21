package io.github.mojira.arisa.domain

import net.rcarz.jiraclient.Issue

data class IssueUpdateContext(
    val update: Issue.FluentUpdate,
    val transition: Issue.FluentTransition,
    var hasUpdates: Boolean = false,
    var hasTransitions: Boolean = false,
    var transitionName: String = "Update Issue"
)
