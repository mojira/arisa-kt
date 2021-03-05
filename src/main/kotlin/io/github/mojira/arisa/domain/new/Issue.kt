package io.github.mojira.arisa.domain.new

import java.time.Instant

data class Issue(
    val key: String,
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
    val affectedVersions: List<Version>,
    val fixVersions: List<Version>,
    val attachments: List<Attachment>,
    val originalComments: List<Comment>,
    val links: List<Link>,
    val changeLog: List<ChangeLogItem>,
    val originalIssue: Issue?,
) {
    var addedComments = mutableListOf<Comment>()
    var removedComments = mutableListOf<String>()
    var editedComments = mutableListOf<Comment>()

    val comments: List<Comment>
        get() = originalComments
            .filter { comment -> removedComments.contains(comment.id) || editedComments.any { it.id == comment.id } }
            .plus(editedComments)
            .plus(addedComments)
            .sortedBy { it.created }
}