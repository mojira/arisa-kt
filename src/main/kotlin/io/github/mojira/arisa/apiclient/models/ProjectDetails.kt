@file:Suppress("MaxLineLength")

package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Details about a project.
 *
 * @param avatarUrls The URLs of the project's avatars.
 * @param id The ID of the project.
 * @param key The key of the project.
 * @param name The name of the project.
 * @param projectCategory The category the project belongs to.
 * @param projectTypeKey The [project type](https://confluence.atlassian.com/x/GwiiLQ#Jiraapplicationsoverview-Productfeaturesandprojecttypes) of the project.
 * @param self The URL of the project details.
 * @param simplified Whether or not the project is simplified.
 */
@Serializable
data class ProjectDetails(
    val avatarUrls: AvatarUrlsBean? = null,
    val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val projectCategory: UpdatedProjectCategory? = null,
    val projectTypeKey: ProjectTypeKey? = null,
    val self: String? = null,
    val simplified: Boolean? = null
) {
    /**
     * The project type of the project.
     */
    @Serializable
    enum class ProjectTypeKey(
        val value: String
    ) {
        @SerialName("software")
        Software("software"),

        @SerialName("service_desk")
        ServiceDesk("service_desk"),

        @SerialName("business")
        Business("business")
    }
}
