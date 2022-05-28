import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    id("org.springframework.boot") version "2.7.0"
}

group = "com.github.mrbean355"
version = "1.15.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.7.0")
    implementation("org.postgresql:postgresql:42.3.6")
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.telegram:telegrambots:6.0.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.0.1")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.12.4")
}

tasks.test {
    useJUnitPlatform()
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    file("src/main/kotlin/com/github/mrbean355/zakbot/BuildConfig.kt").writeText(
        "package com.github.mrbean355.zakbot\n" +
                "\n" +
                "const val AppVersion = \"$version\""
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(generateBuildConfig)
}