plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
    application
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
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

val logBackVersion = "1.4.3"
val arrowVersion = "0.10.4"
val kotestVersion = "4.4.1"

dependencies {
    implementation("com.uchuhimo", "konf", "1.1.2")
    implementation("com.github.rcarz", "jira-client", "868a5ca897")
    implementation("com.urielsalis", "mc-crash-lib", "2.0.10")
    implementation("com.github.napstr", "logback-discord-appender", "bda874138e")
    implementation("org.slf4j", "slf4j-api", "2.0.3")
    implementation("ch.qos.logback", "logback-classic", logBackVersion)
    implementation("ch.qos.logback", "logback-core", logBackVersion)
    implementation("io.arrow-kt", "arrow-core", arrowVersion)
    implementation("io.arrow-kt", "arrow-syntax", arrowVersion)
    implementation("io.arrow-kt", "arrow-fx", arrowVersion)
    implementation("com.beust", "klaxon", "5.6")
    implementation("com.mojang", "brigadier", "1.0.18")
    implementation("org.apache.commons", "commons-imaging", "1.0-alpha3")

    components {
        /*
         * Clears all dependencies of dom4j (which is used transitively) because they are actually optional
         * dependencies, see:
         * - https://github.com/dom4j/dom4j/issues/99#issuecomment-830063159
         * - https://github.com/dom4j/dom4j/blob/7fbdc6d58623ec0b54b9b44a4738781b65191df1/build.gradle#L92-L96
         *
         * One of the optional dependencies (pull-parser) is causing issues because it registers itself as
         * javax.xml.parsers.SAXParserFactory implementation, but it does not support all features, causing
         * failures for logback.
         */
        withModule<ClearDependencies>("org.dom4j:dom4j")
    }

    testImplementation("io.kotest", "kotest-assertions-core-jvm", kotestVersion)
    testImplementation("io.kotest", "kotest-runner-junit5", kotestVersion)
    testImplementation("io.kotest", "kotest-assertions-arrow", kotestVersion)
    testImplementation("io.mockk", "mockk", "1.13.2")
    testImplementation("org.reflections", "reflections", "0.10.2")
}

// Used for removing dependencies of dom4j, see usage above
class ClearDependencies : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants { withDependencies { clear() } }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks =
            (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1 // Run with same number of cores
        reports.html.required.set(false)
        reports.junitXml.required.set(false)
    }
}

application {
    mainClass.set("io.github.mojira.arisa.ArisaMainKt")
}

detekt {
    // For now only consider main source files, but ignore test sources
    source.setFrom(files("src/main/kotlin"))
    allRules = true // enable all rules, including unstable ones
    buildUponDefaultConfig = true // preconfigure defaults
    parallel = true
}

tasks {
    withType<io.gitlab.arturbosch.detekt.Detekt> {
        reports {
            html.required.set(false) // Disabled due to requirement for kotlinx-html which is still in jcenter
            xml.required.set(false) // checkstyle like format mainly for integrations like Jenkins
            txt.required.set(false) // similar to the console output, contains issue signature to manually edit baseline files
        }
    }
}
