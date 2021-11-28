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
import java.io.File
import java.io.InputStream
import java.time.Instant

val RIGHT_NOW: Instant = Instant.now()
const val PRIVATE_SECURITY_LEVEL = "private"

fun mockAttachment(
    id: String = "0",
    name: String = "",
    created: Instant = RIGHT_NOW,
    mimeType: String = "text/plain",
    remove: () -> Unit = { },
    openInputStream: () -> InputStream = { InputStream.nullInputStream() },
    getContent: () -> ByteArray = { ByteArray(0) },
    uploader: User = mockUser()
) = Attachment(
    id,
    name,
    created,
    mimeType,
    remove,
    openInputStream,
    getContent,
    uploader
)

fun mockChangeLogItem(
    entryId: String = "1",
    itemIndex: Int = 0,
    created: Instant = RIGHT_NOW,
    field: String = "",
    changedFrom: String? = null,
    changedFromString: String? = null,
    changedTo: String? = null,
    changedToString: String? = null,
    author: User = mockUser(),
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = ChangeLogItem(
    entryId,
    itemIndex,
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
    id: String = "0",
    body: String = "",
    author: User = mockUser(),
    getAuthorGroups: () -> List<String> = { emptyList() },
    created: Instant = RIGHT_NOW,
    updated: Instant = created,
    visibilityType: String? = null,
    visibilityValue: String? = null,
    restrict: (String) -> Unit = { },
    update: (String) -> Unit = { },
    remove: () -> Unit = { }
) = Comment(
    id,
    body,
    author,
    getAuthorGroups,
    created,
    updated,
    visibilityType,
    visibilityValue,
    restrict,
    update,
    remove
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
    platform: String? = null,
    dungeonsPlatform: String? = null,
    project: Project = mockProject(),
    affectedVersions: List<Version> = emptyList(),
    fixVersions: List<Version> = emptyList(),
    attachments: List<Attachment> = emptyList(),
    comments: List<Comment> = emptyList(),
    links: List<Link> = emptyList(),
    changeLog: List<ChangeLogItem> = emptyList(),
    reopen: () -> Unit = { },
    resolveAsAwaitingResponse: () -> Unit = { },
    resolveAsInvalid: () -> Unit = { },
    resolveAsDuplicate: () -> Unit = { },
    resolveAsIncomplete: () -> Unit = { },
    updateDescription: (description: String) -> Unit = { },
    updateCHK: () -> Unit = { },
    updateConfirmationStatus: (String) -> Unit = { },
    updatePlatform: (String) -> Unit = { },
    updateDungeonsPlatform: (String) -> Unit = { },
    updateLinked: (Double) -> Unit = { },
    setPrivate: () -> Unit = { },
    addAffectedVersionById: (id: String) -> Unit = { },
    addAffectedVersion: (version: Version) -> Unit = { },
    removeAffectedVersion: (version: Version) -> Unit = { },
    createLink: (key: String, type: String, outwards: Boolean) -> Unit = { _, _, _ -> },
    addComment: (options: CommentOptions) -> Unit = { },
    addDupeMessage: (options: CommentOptions) -> Unit = { },
    addRestrictedComment: (options: CommentOptions) -> Unit = { },
    addNotEnglishComment: (language: String) -> Unit = { },
    addRawRestrictedComment: (body: String, restriction: String) -> Unit = { _, _ -> },
    addRawBotComment: (rawBody: String) -> Unit = { },
    markAsFixedInASpecificVersion: (versionName: String) -> Unit = { },
    changeReporter: (reporter: String) -> Unit = { },
    addAttachmentFromFile: (file: File, cleanupCallback: () -> Unit) -> Unit = { _, cleanupCallback -> cleanupCallback() },
    addAttachment: (name: String, content: String) -> Unit = { _, _ -> }
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
    platform,
    dungeonsPlatform,
    affectedVersions,
    fixVersions,
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
    updatePlatform,
    updateDungeonsPlatform,
    updateLinked,
    setPrivate,
    addAffectedVersionById,
    addAffectedVersion,
    removeAffectedVersion,
    createLink,
    addComment,
    addDupeMessage,
    addRestrictedComment,
    addNotEnglishComment,
    addRawRestrictedComment,
    addRawBotComment,
    markAsFixedInASpecificVersion,
    changeReporter,
    addAttachmentFromFile,
    addAttachment
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
    createLink: (key: String, type: String, outwards: Boolean) -> Unit = { _, _, _ -> }
) = LinkedIssue(
    key,
    status,
    getFullIssue,
    createLink
)

fun mockProject(
    key: String = "MC",
    versions: List<Version> = emptyList(),
    privateSecurity: String = PRIVATE_SECURITY_LEVEL
) = Project(
    key,
    versions,
    privateSecurity
)

fun mockUser(
    name: String = "user",
    displayName: String = "User",
    getGroups: () -> List<String>? = { null },
    isNewUser: () -> Boolean = { false },
    isBotUser: () -> Boolean = { false }
) = User(
    name,
    displayName,
    getGroups,
    isNewUser,
    isBotUser
)

fun mockVersion(
    id: String = "",
    name: String = "name",
    released: Boolean = true,
    archived: Boolean = false,
    releaseDate: Instant? = RIGHT_NOW
) = Version(
    id,
    name,
    released,
    archived,
    releaseDate
)
