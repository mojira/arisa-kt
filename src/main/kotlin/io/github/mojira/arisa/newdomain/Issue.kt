package io.github.mojira.arisa.newdomain

import java.time.Instant

data class Issue(
    val key: String,
    var summary: String = "",
    var status: String?,
    var description: String = "",
    var environment: String = "",
    var securityLevel: String?,
    var reporter: User?,
    var resolution: String?,
    val created: Instant,
    var updated: Instant?,
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
    val newAffectedVersions: MutableList<Version> = mutableListOf(),
    val originalFixVersions: List<Version>,
    val newFixVersions: MutableList<Version> = mutableListOf(),
    val originalAttachments: List<Attachment>,
    val newAttachments: MutableList<Attachment> = mutableListOf(),
    val originalComments: List<Comment>,
    val newComments: MutableList<Comment> = mutableListOf(),
    val originalLinks: List<Link>,
    val newLinks: MutableList<Link> = mutableListOf(),
    val changeLog: List<ChangeLogItem>
)
