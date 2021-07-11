package io.github.mojira.arisa.newdomain

import java.time.Instant

data class Issue(
    val key: String,
    var summary: String = "",
    var status: String,
    var description: String = "",
    var environment: String = "",
    var securityLevel: String?,
    var reporter: User?,
    var resolution: String?,
    val created: Instant,
    var updated: Instant,
    var resolved: Instant?,
    var chk: String?,
    var confirmationStatus: String?,
    var linked: Double?,
    var priority: String?,
    val triagedTime: String?,
    val project: Project,
    var platform: String?,
    var dungeonsPlatform: String?,
    val originalAffectedVersions: List<Version>,
    val affectedVersions: MutableList<Version>,
    val originalFixVersions: List<Version>,
    val fixVersions: MutableList<Version>,
    val originalAttachments: List<Attachment>,
    val attachments: MutableList<Attachment>,
    val originalComments: List<Comment>,
    val comments: MutableList<Comment>,
    val originalLinks: List<Link>,
    val links: MutableList<Link>,
    val changeLog: List<ChangeLogItem>
)
