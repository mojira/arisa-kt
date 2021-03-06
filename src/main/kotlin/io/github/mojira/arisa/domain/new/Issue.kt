package io.github.mojira.arisa.domain.new

import java.time.Instant

data class Issue(
    val key: String,
    val summary: String?,
    val status: String,
    val description: String?,
    val environment: String?,
    var securityLevel: String?,
    val reporter: User?,
    var resolution: String?,
    val created: Instant,
    val updated: Instant,
    val resolved: Instant?,
    var chk: String?,
    val confirmationStatus: String?,
    var linked: Double?,
    val priority: String?,
    val triagedTime: String?,
    val project: Project,
    val platform: String?,
    val affectedVersions: List<Version>,
    val fixVersions: List<Version>,
    val attachments: List<Attachment>,
    val originalComments: List<Comment>,
    val links: List<Link>,
    val changeLog: List<ChangeLogItem>,
    val originalIssue: Issue?,
) {
    var addedComments = mutableListOf<Comment>()
    var editedComments = mutableListOf<Comment>()

    val comments: List<Comment>
        get() = originalComments
            .plus(addedComments)
            .filter { comment -> editedComments.any { it.id == comment.id } }
            .plus(editedComments)
            .sortedBy { it.created }
}