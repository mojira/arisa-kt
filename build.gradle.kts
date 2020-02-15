plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

group = "io.github.mojira"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val logBackVersion = "1.2.3"
val arrowVersion = "0.10.4"
val kotlinTestVersion = "3.4.2"

dependencies {
    implementation(kotlin("stdlib-jdk8") as String) {
        isForce = true
        isChanging = true
    }
    implementation(kotlin("reflect") as String) {
        isForce = true
        isChanging = true
    }

    implementation("com.uchuhimo", "konf", "0.22.1")
    implementation("net.rcarz", "jira-client", "0.5")
    implementation("org.slf4j", "slf4j-api", "1.7.25")
    implementation("ch.qos.logback", "logback-classic", logBackVersion)
    implementation("ch.qos.logback", "logback-core", logBackVersion)
    implementation("io.arrow-kt", "arrow-core", arrowVersion)
    implementation("io.arrow-kt", "arrow-syntax", arrowVersion)
    implementation("io.arrow-kt", "arrow-fx", arrowVersion)

    testImplementation("io.kotlintest", "kotlintest-runner-junit5", kotlinTestVersion) {
        exclude("io.arrow-kt") // Need to manually exclude until https://github.com/kotlintest/kotlintest/issues/1026 is fixed in 3.4.3
    }
    implementation("io.kotlintest", "kotlintest-assertions-arrow", kotlinTestVersion) {
        exclude("io.arrow-kt") // Need to manually exclude until https://github.com/kotlintest/kotlintest/issues/1026 is fixed in 3.4.3
    }
    testImplementation("io.mockk", "mockk", "1.9.3")
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
    }
}
