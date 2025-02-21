package io.github.mojira.arisa.domain

import arrow.core.Either
import io.github.mojira.arisa.infrastructure.apiclient.builders.FluentObjectBuilder
import net.rcarz.jiraclient.JiraClient
import io.github.mojira.arisa.infrastructure.apiclient.JiraClient as MojiraClient
import net.rcarz.jiraclient.Issue as JiraIssue
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueBean as MojiraIssue

/**
 * Terminology: `edit`, `update`, and `resolve` are the same terms used on the Mojira interface.
 */
data class IssueUpdateContext(
    val jiraClient: MojiraClient,
    val jiraIssue: MojiraIssue,
    val edit: FluentObjectBuilder,
    val update: FluentObjectBuilder,
    val resolve: FluentObjectBuilder,
    var hasEdits: Boolean = false,
    var hasUpdates: Boolean = false,
    var transitionName: String? = null,
    val otherOperations: MutableList<() -> Either<Throwable, Unit>> =
        emptyList<() -> Either<Throwable, Unit>>().toMutableList(),
    var triggeredBy: String? = null
)
