package io.github.mojira.arisa.utils

import arrow.core.Either
import arrow.core.right
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.infrastructure.jira.CommentOptions
import io.github.mojira.arisa.modules.NOW
import java.time.Instant

fun getIssue(
    key: String = "MC-1",
    summary: String? = null,
    status: String = "Open",
    description: String? = null,
    environment: String? = null,
    securityLevel: String? = null,
    reporter: User? = null,
    resolution: String? = null,
    created: Instant = Instant.now(),
    updated: Instant = Instant.now(),
    chk: String? = null,
    confirmationStatus: String? = null,
    linked: Double? = null,
    priority: String? = null,
    triagedTime: String? = null,
    project: Project = getProject(),
    affectedVersions: List<Version> = emptyList(),
    attachments: List<Attachment> = emptyList(),
    comments: List<Comment> = emptyList(),
    links: List<Link> = emptyList(),
    changeLog: List<ChangeLogItem> = emptyList(),
    reopen: () -> Either<Throwable, Unit> = { Unit.right() },
    resolveAsAwaitingResponse: () -> Either<Throwable, Unit> = { Unit.right() },
    resolveAsInvalid: () -> Either<Throwable, Unit> = { Unit.right() },
    resolveAsDuplicate: () -> Either<Throwable, Unit> = { Unit.right() },
    resolveAsIncomplete: () -> Either<Throwable, Unit> = { Unit.right() },
    updateDescription: (description: String) -> Either<Throwable, Unit> = { Unit.right() },
    updateCHK: () -> Either<Throwable, Unit> = { Unit.right() },
    updateConfirmationStatus: (String) -> Either<Throwable, Unit> = { Unit.right() },
    updateLinked: (Double) -> Either<Throwable, Unit> = { Unit.right() },
    setPrivate: () -> Either<Throwable, Unit> = { Unit.right() },
    createLink: (key: String, type: String) -> Either<Throwable, Unit> = { _, _ -> Unit.right() },
    addAffectedVersion: (id: String) -> Either<Throwable, Unit> = { Unit.right() },
    addComment: (options: CommentOptions) -> Either<Throwable, Unit> = { Unit.right() },
    addRestrictedComment: (options: CommentOptions) -> Either<Throwable, Unit> = { Unit.right() },
    addNotEnglishComment: (language: String) -> Either<Throwable, Unit> = { Unit.right() }
) = Issue(
    key,
    summary,
    status,
    description,
    environment,
    securityLevel,
    reporter,
    resolution,
    created,
    updated,
    chk,
    confirmationStatus,
    linked,
    priority,
    triagedTime,
    project,
    affectedVersions,
    attachments,
    comments,
    links,
    changeLog,
    reopen,
    resolveAsAwaitingResponse,
    resolveAsInvalid,
    resolveAsDuplicate,
    resolveAsIncomplete,
    updateDescription,
    updateCHK,
    updateConfirmationStatus,
    updateLinked,
    setPrivate,
    createLink,
    addAffectedVersion,
    addComment,
    addRestrictedComment,
    addNotEnglishComment
)

fun getProject(
    key: String = "MC",
    versions: List<Version> = emptyList(),
    privateSecurity: String = "private"
) = Project(
    key,
    versions,
    privateSecurity
)

fun getAttachment(
    name: String = "",
    created: Instant = Instant.now(),
    remove: () -> Either<Throwable, Unit> = { Unit.right() },
    getContent: () -> ByteArray = { ByteArray(0) }
) = Attachment(
    name,
    created,
    remove,
    getContent
)
