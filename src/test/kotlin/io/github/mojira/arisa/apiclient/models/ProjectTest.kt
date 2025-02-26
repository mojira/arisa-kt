package io.github.mojira.arisa.apiclient.models

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.time.LocalDate

class ProjectTest : StringSpec({
    "it should deserialize project with versions" {
        val json = loadJsonFile("project-with-versions.json")
        val project = JSON.decodeFromString<Project>(json)

        project shouldNotBe null
        project.versions shouldHaveSize 3
        project.versions.shouldBeTypeOf<ArrayList<Version>>()
        val versionWithDates = project.versions.first { it.startDate != null }
        versionWithDates.startDate shouldBe LocalDate.of(2025, 2, 25)
        versionWithDates.releaseDate shouldBe LocalDate.of(2025, 3, 2)
    }
})
