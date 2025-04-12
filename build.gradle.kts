val tgbotapi_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.khannan"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.inmo:tgbotapi:$tgbotapi_version")
    implementation("dev.inmo:tgbotapi.core:$tgbotapi_version")
    implementation("dev.inmo:tgbotapi.api:$tgbotapi_version")
    implementation("dev.inmo:tgbotapi.utils:$tgbotapi_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC.2")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    testImplementation(kotlin("test"))
    implementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(21)
}

