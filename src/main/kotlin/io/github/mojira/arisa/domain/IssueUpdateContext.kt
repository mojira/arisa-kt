package io.github.mojira.arisa.domain

import arrow.core.Either
import net.rcarz.jiraclient.Issue

/**
 * Terminology: `edit`, `update`, and `resolve` are the same terms used on the Mojira interface.
 */
data class IssueUpdateContext(
    val edit: Issue.FluentUpdate,
    val update: Issue.FluentTransition,
    val resolve: Issue.FluentTransition,
    var hasEdits: Boolean = false,
    var hasUpdates: Boolean = false,
    var transitionName: String? = null,
    val otherOperations: List<() -> Either<Throwable, Unit>> = emptyList()
)
