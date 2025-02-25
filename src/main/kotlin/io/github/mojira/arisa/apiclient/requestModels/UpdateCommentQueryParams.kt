package io.github.mojira.arisa.apiclient.requestModels

data class UpdateCommentQueryParams(
    val notifyUsers: Boolean? = null,
    val overrideEditableFlag: Boolean? = null,
    val expand: String? = null
) {
    fun toMap(): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            notifyUsers?.let { put("notifyUsers", it.toString()) }
            overrideEditableFlag?.let { put("overrideEditableFlag", it.toString()) }
            expand?.let { put("expand", it) }
        }
    }
}