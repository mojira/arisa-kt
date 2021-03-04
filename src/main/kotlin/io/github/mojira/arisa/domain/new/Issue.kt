package io.github.mojira.arisa.domain.new

import java.time.Instant

data class Issue(
    var key: String,
    var summary: String?,
    var status: String,
    var description: String?,
    var environment: String?,
    var securityLevel: String?,
    var reporter: User?,
    var resolution: String?,
    var created: Instant,
    var updated: Instant,
    var resolved: Instant?,
    var chk: String?,
    var confirmationStatus: String?,
    var linked: Double?,
    var priority: String?,
    var triagedTime: String?,
    var project: Project,
    var platform: String?,
    var affectedVersions: List<Version>,
    var fixVersions: List<Version>,
    var attachments: List<Attachment>,
    var comments: List<Comment>,
    var links: List<Link>,
    var changeLog: List<ChangeLogItem>,
    val originalIssue: Issue?
)