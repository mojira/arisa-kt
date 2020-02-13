plugins {
    kotlin("jvm") version "1.3.61"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    compile(group = "com.uchuhimo", name = "konf", version = "0.22.1")
    compile("net.rcarz", "jira-client", "0.5")

    implementation("org.slf4j", "slf4j-api", "1.7.25")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("ch.qos.logback", "logback-core", "1.2.3")
    implementation("io.arrow-kt", "arrow-core", "0.10.4")
    implementation("io.arrow-kt", "arrow-syntax", "0.10.4")
    implementation("io.arrow-kt", "arrow-fx", "0.10.4")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}