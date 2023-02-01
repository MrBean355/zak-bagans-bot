import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.springframework.boot") version "3.0.2"
}

group = "com.github.mrbean355"
version = "2.3.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:3.0.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.0.2")
    implementation("org.postgresql:postgresql:42.5.2")
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.telegram:telegrambots:6.4.0")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.4.0")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.3")
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