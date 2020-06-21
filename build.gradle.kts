plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    application
    id("io.gitlab.arturbosch.detekt") version "1.9.1"
    id("info.solidsoft.pitest") version "1.5.1"
}

group = "io.github.mojira"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/cbeust/maven")
    jcenter()
}

buildscript {
    repositories {
        mavenCentral()
        jcenter {
            content {
                // just allow to include kotlinx projects
                // detekt needs 'kotlinx-html' for the html report
                includeGroup("org.jetbrains.kotlinx")
            }
        }
    }
    dependencies {
        classpath("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.5.1")
    }
}

val logBackVersion = "1.2.3"
val arrowVersion = "0.10.4"
val kotestVersion = "4.0.2"

dependencies {
    implementation(kotlin("stdlib-jdk8") as String) {
        isForce = true
        isChanging = true
    }
    implementation(kotlin("reflect") as String) {
        isForce = true
        isChanging = true
    }

    testImplementation("io.kotest:kotest-plugins-pitest-jvm:$kotestVersion")
    pitest("com.github.pitest:pitest-kotlin:master-SNAPSHOT")

    implementation("com.uchuhimo", "konf", "0.22.1")
    implementation("com.github.rcarz", "jira-client", "master-SNAPSHOT")
    implementation("me.urielsalis", "mc-crash-lib", "master-SNAPSHOT")
    implementation("org.slf4j", "slf4j-api", "1.7.25")
    implementation("ch.qos.logback", "logback-classic", logBackVersion)
    implementation("ch.qos.logback", "logback-core", logBackVersion)
    implementation("io.arrow-kt", "arrow-core", arrowVersion)
    implementation("io.arrow-kt", "arrow-syntax", arrowVersion)
    implementation("io.arrow-kt", "arrow-fx", arrowVersion)
    implementation("com.beust", "klaxon", "5.2")

    testImplementation("io.kotest", "kotest-assertions-core-jvm", kotestVersion)
    testImplementation("io.kotest", "kotest-runner-junit5", kotestVersion)
    testImplementation("io.kotest", "kotest-assertions-arrow", kotestVersion)
    testImplementation("io.mockk", "mockk", "1.9.3")
    testImplementation("org.reflections", "reflections", "0.9.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform()
        maxParallelForks =
            (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1 // Run with same number of cores
        reports.html.isEnabled = false
        reports.junitXml.isEnabled = false
    }
}

application {
    mainClassName = "io.github.mojira.arisa.ArisaMainKt"
}

detekt {
    failFast = true // fail build on any finding
    buildUponDefaultConfig = true // preconfigure defaults
    parallel = true
    reports {
        html {
            destination = file("build/reports/detekt.html")
            enabled = true // observe findings in your browser with structure and code snippets
        }
        xml.enabled = false // checkstyle like format mainly for integrations like Jenkins
        txt.enabled = false // similar to the console output, contains issue signature to manually edit baseline files
    }
}

tasks {
    withType<io.gitlab.arturbosch.detekt.Detekt> {
        // Target version of the generated JVM bytecode. It is used for type resolution.
        this.jvmTarget = "1.8"
    }
}

configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    testPlugin.set("Kotest")
    targetClasses.set(listOf("io.github.mojira.*"))
    outputFormats.add("HTML")
    outputFormats.add("XML")
}
