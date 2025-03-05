package io.github.mojira.arisa.utils

import arrow.core.Either
import arrow.core.right
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.domain.cloud.CloudLink
import io.github.mojira.arisa.domain.cloud.CloudLinkedIssue
import java.io.File
import java.time.Instant

fun mockCloudIssue(
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
    project: Project = mockProject(),
    attachments: List<Attachment> = emptyList(),
    comments: List<Comment> = emptyList(),
    links: List<CloudLink> = emptyList(),
    changeLog: List<ChangeLogItem> = emptyList(),
    reopen: () -> Unit = { },
    setPrivate: () -> Unit = { },
    addComment: (options: CommentOptions) -> Unit = { },
    addRawRestrictedComment: (body: String, restriction: String) -> Unit = { _, _ -> },
    addRawBotComment: (rawMessage: String) -> Unit = { },
    addAttachmentFromFile: (file: File, cleanupCallback: () -> Unit) -> Unit = { _, cleanupCallback -> cleanupCallback() }
) = CloudIssue(
    key = key,
    summary = summary,
    status = status,
    description = description,
    environment = environment,
    securityLevel = securityLevel,
    resolution = resolution,
    reporter = reporter,
    created = created,
    updated = updated,
    project = project,
    attachments = attachments,
    comments = comments,
    links = links,
    changeLog = changeLog,
    reopen = reopen,
    setPrivate = setPrivate,
    addComment = addComment,
    addRawRestrictedComment = addRawRestrictedComment,
    addRawBotComment = addRawBotComment,
    addAttachmentFromFile = addAttachmentFromFile
)

fun mockCloudLinkedIssue(
    key: String = "MC-1",
    status: String = "Open",
    getFullIssue: () -> Either<Throwable, CloudIssue> = { mockCloudIssue().right() },
    createLink: (key: String, type: String, outwards: Boolean) -> Unit = { _, _, _ -> }
) = CloudLinkedIssue(
    key,
    status,
    getFullIssue,
    createLink
)

fun mockCloudLink(
    type: String = "Duplicate",
    outwards: Boolean = true,
    issue: CloudLinkedIssue = mockCloudLinkedIssue(),
    remove: () -> Unit = { Unit.right() }
) = CloudLink(
    type,
    outwards,
    issue,
    remove
)
