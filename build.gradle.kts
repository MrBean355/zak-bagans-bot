import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarqube.gradle.SonarTask

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    kotlin("plugin.jpa") version "2.4.20"
    id("org.springframework.boot") version "4.1.1"
    id("org.sonarqube") version "7.5.0.8588"
    jacoco
    `jvm-test-suite`
}

group = "com.github.mrbean355"
version = "2.13.1"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(25)
    targetCompatibility = JavaVersion.toVersion(25)
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.test)
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
    }
}

tasks.withType<SonarTask> {
    dependsOn(tasks.named("jacocoTestReport"))
}

sonar {
    properties {
        property("sonar.projectKey", "MrBean355_zak-bagans-bot")
        property("sonar.organization", "mrbean355")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("zakbot.jar")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:4.1.1")
    implementation("org.springframework.boot:spring-boot-starter-validation:4.1.1")
    implementation("org.springframework.boot:spring-boot-starter-security:4.1.1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.faendir.jraw:JRAW:1.2.0")
    implementation("org.telegram:telegrambots-client:10.3.0")
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:10.3.0")
    implementation("org.commonmark:commonmark:0.30.0")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.0")
            dependencies {
                implementation("io.mockk:mockk:1.14.11")
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