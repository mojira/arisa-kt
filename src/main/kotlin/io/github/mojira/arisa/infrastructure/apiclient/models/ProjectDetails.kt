package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


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
data class ProjectDetails (
    /* The URLs of the project's avatars. */
    @SerialName("avatarUrls")
    val avatarUrls: AvatarUrlsBean? = null,

    /* The ID of the project. */
    @SerialName("id")
    val id: String? = null,

    /* The key of the project. */
    @SerialName("key")
    val key: String? = null,

    /* The name of the project. */
    @SerialName("name")
    val name: String? = null,

    /* The category the project belongs to. */
    @SerialName("projectCategory")
    val projectCategory: UpdatedProjectCategory? = null,

    /* The project type of the project. */
    @SerialName("projectTypeKey")
    val projectTypeKey: ProjectTypeKey? = null,

    /* The URL of the project details. */
    @SerialName("self")
    val self: String? = null,

    /* Whether or not the project is simplified. */
    @SerialName("simplified")
    val simplified: Boolean? = null
) {
    /**
     * The project type of the project.
     */
    @Serializable
    enum class ProjectTypeKey(val value: String) {
        @SerialName("software")
        software("software"),

        @SerialName("service_desk")
        service_desk("service_desk"),

        @SerialName("business")
        business("business")
    }
}

