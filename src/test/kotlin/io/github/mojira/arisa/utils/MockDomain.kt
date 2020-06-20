package io.github.mojira.arisa.utils

import arrow.core.Either
import arrow.core.right
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import java.time.Instant

val RIGHT_NOW: Instant = Instant.now()

fun mockAttachment(
    name: String = "",
    created: Instant = RIGHT_NOW,
    remove: () -> Unit = { Unit },
    getContent: () -> ByteArray = { ByteArray(0) }
) = Attachment(
    name,
    created,
    remove,
    getContent
)

fun mockChangeLogItem(
    created: Instant = RIGHT_NOW,
    field: String = "",
    changedFrom: String? = null,
    changedFromString: String? = null,
    changedTo: String? = null,
    changedToString: String? = null,
    author: User = mockUser(),
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = ChangeLogItem(
    created,
    field,
    changedFrom,
    changedFromString,
    changedTo,
    changedToString,
    author,
    getAuthorGroups
)

fun mockComment(
    body: String = "",
    author: User = mockUser(),
    getAuthorGroups: () -> List<String> = { emptyList() },
    created: Instant = RIGHT_NOW,
    updated: Instant = created,
    visibilityType: String? = null,
    visibilityValue: String? = null,
    restrict: (String) -> Unit = { Unit },
    update: (String) -> Unit = { Unit }
) = Comment(
    body,
    author,
    getAuthorGroups,
    created,
    updated,
    visibilityType,
    visibilityValue,
    restrict,
    update
)

fun mockIssue(
    key: String = "MC-1",
    summary: String? = null,
    status: String = "Open",
    description: String? = null,
    environment: String? = null,
    securityLevel: String? = null,
    reporter: User? = null,
    resolution: String? = null,
    created: Instant = RIGHT_NOW,
    updated: Instant = RIGHT_NOW,
    resolved: Instant? = null,
    chk: String? = null,
    confirmationStatus: String? = null,
    linked: Double? = null,
    priority: String? = null,
    triagedTime: String? = null,
    project: Project = mockProject(),
    affectedVersions: List<Version> = emptyList(),
    attachments: List<Attachment> = emptyList(),
    comments: List<Comment> = emptyList(),
    links: List<Link> = emptyList(),
    changeLog: List<ChangeLogItem> = emptyList(),
    reopen: () -> Unit = { Unit },
    resolveAsAwaitingResponse: () -> Unit = { Unit },
    resolveAsInvalid: () -> Unit = { Unit },
    resolveAsDuplicate: () -> Unit = { Unit },
    resolveAsIncomplete: () -> Unit = { Unit },
    updateDescription: (description: String) -> Unit = { Unit },
    updateCHK: () -> Unit = { Unit },
    updateConfirmationStatus: (String) -> Unit = { Unit },
    updateLinked: (Double) -> Unit = { Unit },
    setPrivate: () -> Unit = { Unit },
    addAffectedVersion: (id: String) -> Unit = { Unit },
    createLink: (key: String, type: String) -> Unit = { _, _ -> Unit },
    addComment: (options: CommentOptions) -> Unit = { Unit },
    addRestrictedComment: (options: CommentOptions) -> Unit = { Unit },
    addNotEnglishComment: (language: String) -> Unit = { Unit },
    addRawRestrictedComment: (body: String, restrictions: String) -> Unit = { _, _ -> Unit },
    markAsFixedInASpecificVersion: (version: String) -> Unit = { Unit }
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
    resolved,
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
    addAffectedVersion,
    createLink,
    addComment,
    addRestrictedComment,
    addNotEnglishComment,
    addRawRestrictedComment,
    markAsFixedInASpecificVersion
)

fun mockLink(
    type: String = "Duplicate",
    outwards: Boolean = true,
    issue: LinkedIssue = mockLinkedIssue(),
    remove: () -> Unit = { Unit.right() }
) = Link(
    type,
    outwards,
    issue,
    remove
)

fun mockLinkedIssue(
    key: String = "MC-1",
    status: String = "Open",
    getFullIssue: () -> Either<Throwable, Issue> = { mockIssue().right() },
    createLink: (key: String, type: String) -> Unit = { _, _ -> Unit }
) = LinkedIssue(
    key,
    status,
    getFullIssue,
    createLink
)

fun mockProject(
    key: String = "MC",
    versions: List<Version> = emptyList(),
    privateSecurity: String = "private"
) = Project(
    key,
    versions,
    privateSecurity
)

fun mockUser(
    name: String = "user",
    displayName: String = "User",
    getGroups: () -> List<String>? = { null }
) = User(
    name,
    displayName,
    getGroups
)

fun mockVersion(
    id: String = "",
    name: String = "name",
    released: Boolean = true,
    archived: Boolean = false,
    releaseDate: Instant? = RIGHT_NOW,
    add: () -> Unit = { Unit },
    remove: () -> Unit = { Unit }
) = Version(
    id,
    name,
    released,
    archived,
    releaseDate,
    add,
    remove
)
