plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    application
    id("io.gitlab.arturbosch.detekt")
    id("info.solidsoft.pitest")
}

group = "io.github.mojira"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Dependencies which can only be found in JitPack repository
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://jitpack.io")
            }
        }
        filter {
            includeModule("com.github.rcarz", "jira-client")
            includeModule("com.github.napstr", "logback-discord-appender")
        }
    }
    // Dependencies which can only be found in Mojang Maven repository
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://libraries.minecraft.net")
            }
        }
        filter {
            // Brigadier is not deployed to Maven Central yet, see https://github.com/Mojang/brigadier/issues/23
            includeModule("com.mojang", "brigadier")
        }
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("info.solidsoft.gradle.pitest:gradle-pitest-plugin:_")
    }
}

val logBackVersion = "1.2.3"
val arrowVersion = "0.10.4"
val kotestVersion = "4.4.3"

dependencies {
    implementation(Kotlin.stdlib.jdk8)
    implementation(kotlin("reflect"))

    implementation(Libs.konf)
    implementation(Libs.jiraClient)
    implementation(Libs.mcCrashLib)
    implementation(Libs.slf4j)
    implementation(Libs.klaxon)
    implementation(Libs.brigadier)
    implementation(Libs.commonsImaging)
    implementation(Libs.Logback.discordAppender)
    implementation(Libs.Logback.core)
    implementation(Libs.Logback.classic)
    implementation(Libs.Arrow.core)
    implementation(Libs.Arrow.fx)
    implementation(Libs.Arrow.syntax)

    testImplementation(Testing.Kotest.assertions.core)
    testImplementation(Testing.Kotest.assertions.arrow)
    testImplementation(Testing.Kotest.runner.junit5)
    testImplementation(Testing.MockK)
    testImplementation(Libs.reflections)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    test {
        useJUnitPlatform()
        maxParallelForks =
            (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1 // Run with same number of cores
    }
}

application {
    mainClass.set("io.github.mojira.arisa.ArisaMainKt")
}

detekt {
    // For now only consider main source files, but ignore test sources
    input = files("src/main/kotlin")
    allRules = true // enable all rules, including unstable ones
    buildUponDefaultConfig = true // preconfigure defaults
    parallel = true
    reports {
        html.enabled = false // Disabled due to requirement for kotlinx-html which is still in jcenter
        xml.enabled = false // checkstyle like format mainly for integrations like Jenkins
        txt.enabled = false // similar to the console output, contains issue signature to manually edit baseline files
    }
}

tasks {
    withType<io.gitlab.arturbosch.detekt.Detekt> {
        // Target version of the generated JVM bytecode. It is used for type resolution.
        this.jvmTarget = "11"
    }
}
