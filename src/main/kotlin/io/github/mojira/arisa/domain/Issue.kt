package io.github.mojira.arisa.domain

import arrow.core.Either
import java.time.Instant

data class Issue(
    val key: String,
    val summary: String?,
    val status: String,
    val description: String?,
    val environment: String?,
    val securityLevel: String?,
    val reporter: User?,
    val resolution: String?,
    val created: Instant,
    val updated: Instant,
    val resolved: Instant?,
    val chk: String?,
    val confirmationStatus: String?,
    val linked: Double?,
    val priority: String?,
    val triagedTime: String?,
    val project: Project,
    val affectedVersions: List<Version>,
    val attachments: List<Attachment>,
    val comments: List<Comment>,
    val links: List<Link>,
    val changeLog: List<ChangeLogItem>,
    val reopen: () -> Unit,
    val resolveAsAwaitingResponse: () -> Unit,
    val resolveAsInvalid: () -> Unit,
    val resolveAsDuplicate: () -> Unit,
    val resolveAsIncomplete: () -> Unit,
    val updateDescription: (description: String) -> Unit,
    val updateCHK: () -> Unit,
    val updateConfirmationStatus: (String) -> Unit,
    val updateLinked: (Double) -> Unit,
    val setPrivate: () -> Unit,
    val addAffectedVersion: (id: String) -> Unit,
    val createLink: (type: String, key: String) -> Either<Throwable, Unit>,
    val addComment: (options: CommentOptions) -> Either<Throwable, Unit>,
    val addRestrictedComment: (options: CommentOptions) -> Either<Throwable, Unit>,
    val addNotEnglishComment: (language: String) -> Either<Throwable, Unit> // Will be removed once we enable the module
)
