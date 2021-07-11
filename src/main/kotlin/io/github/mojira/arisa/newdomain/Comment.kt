package io.github.mojira.arisa.newdomain

import java.time.Instant

data class Comment(
    var body: String = "",
    val author: User,
    val created: Instant,
    var updated: Instant,
    var visibilityType: String?,
    var visibilityValue: String?
)
