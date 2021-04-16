package io.github.mojira.arisa.domain

import io.github.mojira.arisa.infrastructure.services.HelperMessageService
import java.time.Instant

data class Issue(
    val key: String,
    val summary: String?,
    val status: String,
    var description: String?,
    val environment: String?,
    var securityLevel: String?,
    var reporter: User?,
    var resolution: String?,
    val created: Instant,
    val updated: Instant,
    val resolved: Instant?,
    var chk: String?,
    var confirmationStatus: String?,
    var linked: Double?,
    val priority: String?,
    val triagedTime: String?,
    val project: Project,
    var platform: String?,
    var originalAffectedVersions: List<Version>,
    var fixVersions: List<Version>,
    var originalAttachments: List<Attachment>,
    val originalComments: List<Comment>,
    val originalLinks: List<Link>,
    val changeLog: List<ChangeLogItem>,
    val originalIssue: Issue?,
) {
    val addedComments = mutableListOf<Comment>()
    val editedComments = mutableListOf<Comment>()
    val newLinks = mutableListOf<Link>()
    val removedLinks = mutableListOf<Link>()
    val removedAttachments = mutableListOf<Attachment>()
    val addedAffectedVersions = mutableListOf<Version>()
    val removedAffectedVersions = mutableListOf<Version>()

    val links: List<Link>
        get() = originalLinks
            .plus(newLinks)
            .filter { link -> removedLinks.any { it.id != null && it.id == link.id } }

    val comments: List<Comment>
        get() = originalComments
            .plus(addedComments)
            .filter { comment -> editedComments.any { it.id == comment.id } }
            .plus(editedComments)
            .sortedBy { it.created }

    val attachments: List<Attachment>
        get() = originalAttachments
            .filter { link -> removedAttachments.any { it.id == link.id } }

    val affectedVersions: List<Version>
        get() = originalAffectedVersions
            .plus(addedAffectedVersions)
            .filter { version -> removedAffectedVersions.any { it.id == version.id }}

    fun updateChk() {
        chk = "updated!"
    }

    fun addComment(message: String, visType: String? = null, visValue: String? = null, filledText: String? = null, language: String = "en") {
        HelperMessageService.getSingleMessage(project.key, message, filledText = filledText, lang = language).fold(
            { /* TODO what to do */ },
            { addedComments.add(Comment(null, it, null, Instant.now(), null, visType, visValue)) }
        )
    }

    fun addRawComment(message: String, visType: String? = null, visValue: String? = null) {
        addedComments.add(Comment(null, message, null, Instant.now(), null, visType, visValue))
    }

    fun addLink(type: String, outwards: Boolean, key: String) {
        newLinks.add(Link(null, type, outwards, LinkedIssue(key, null, null)))
    }
}