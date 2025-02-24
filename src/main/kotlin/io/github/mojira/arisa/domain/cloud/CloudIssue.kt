package io.github.mojira.arisa.domain.cloud

import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Project
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.domain.Version
import java.io.File
import java.time.Instant

data class CloudIssue(
    val key: String,
    val summary: String?,
    val status: String,
    val description: String?,
    val environment: String?,
    val securityLevel: String?,
//    val reporter: User?,
    val resolution: String?,
    val created: Instant,
//    val updated: Instant,
//    val resolved: Instant?,
//    val chk: String?,
//    val confirmationStatus: String?,
//    val linked: Double?,
//    val priority: String?,
//    val triagedTime: String?,
    val project: Project,
//    val platform: String?,
//    val dungeonsPlatform: String?,
//    val legendsPlatform: String?,
//    val affectedVersions: List<Version>,
//    val fixVersions: List<Version>,
    val attachments: List<Attachment>,
    val comments: List<Comment>,
    val links: List<CloudLink>,
    val changeLog: List<ChangeLogItem>,
//    val reopen: () -> Unit,
//    val resolveAsAwaitingResponse: () -> Unit,
//    val resolveAsInvalid: () -> Unit,
//    val resolveAsDuplicate: () -> Unit,
//    val resolveAsIncomplete: () -> Unit,
//    val updateDescription: (description: String) -> Unit,
//    val updateCHK: () -> Unit,
//    val updateConfirmationStatus: (String) -> Unit,
//    val updatePriority: (String) -> Unit,
//    val updatePlatform: (String) -> Unit,
//    val updateDungeonsPlatform: (String) -> Unit,
//    val updateLegendsPlatform: (String) -> Unit,
//    val updateLinked: (Double) -> Unit,
    val setPrivate: () -> Unit,
//    val addAffectedVersionById: (id: String) -> Unit,
//    val addAffectedVersion: (version: Version) -> Unit,
//    val removeAffectedVersion: (version: Version) -> Unit,
//    val createLink: (type: String, key: String, outwards: Boolean) -> Unit,
    val addComment: (options: CommentOptions) -> Unit,
//    val addDupeMessage: (options: CommentOptions) -> Unit,
//    val addRestrictedComment: (options: CommentOptions) -> Unit,
//    val addNotEnglishComment: (language: String) -> Unit,
    val addRawRestrictedComment: (body: String, restriction: String) -> Unit,
    val addRawBotComment: (rawMessage: String) -> Unit,
//    val markAsFixedWithSpecificVersion: (fixVersionName: String) -> Unit,
//    val changeReporter: (reporter: String) -> Unit,
    val addAttachmentFromFile: (file: File, cleanupCallback: () -> Unit) -> Unit,
//    val addAttachment: (name: String, content: String) -> Unit
)
