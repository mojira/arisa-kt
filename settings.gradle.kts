rootProject.name = "arisa-kt"

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.40.2"
}

refreshVersions {
    enableBuildSrcLibs()
}
