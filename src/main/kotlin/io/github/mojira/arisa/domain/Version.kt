package io.github.mojira.arisa.domain

import java.time.Instant

data class Version(
    val id: String,
    val name: String,
    val released: Boolean,
    val archived: Boolean,
    /**
     * **Important**: Even if [released]=true the release date may be `null`, examples are:
     * "Minecraft 19w03c", "Minecraft 19w13b"
     */
    val releaseDate: Instant?
)
