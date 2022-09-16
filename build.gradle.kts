import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.springframework.boot") version "2.7.3"
}

group = "com.github.mrbean355"
version = "2.0.0"

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
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.7.3")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.telegram:telegrambots:6.1.0")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.1.0")

    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.12.8")
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