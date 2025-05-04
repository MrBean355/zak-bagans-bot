import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.springframework.boot") version "3.4.5"
    `jvm-test-suite`
}

group = "com.github.mrbean355"
version = "2.10.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.4")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.5")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.faendir.jraw:JRAW:1.2.0")
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.9.7.1")
    implementation("org.commonmark:commonmark:0.24.0")

    runtimeOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
    runtimeOnly("javax.xml.ws:jaxws-api:2.3.1") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.0")
            dependencies {
                implementation("io.mockk:mockk:1.13.17")
            }
        }
    }
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