import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.sonarqube.gradle.SonarTask

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"
    id("org.springframework.boot") version "4.1.2"
    id("org.sonarqube") version "7.6.0.18749"
    jacoco
    `jvm-test-suite`
}

group = "com.github.mrbean355"
version = "3.3.0"

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
    toolVersion = "0.8.16"
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

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("zakbot.jar")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:4.1.2")
    implementation("org.springframework.boot:spring-boot-starter-validation:4.1.2")
    implementation("org.springframework.boot:spring-boot-starter-security:4.1.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.2")
    implementation("org.postgresql:postgresql:42.7.14")
    implementation("org.telegram:telegrambots-client:10.3.0")
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:10.3.0")
    implementation("org.commonmark:commonmark:0.32.0")
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter("6.1.3")
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test:4.1.2")
                implementation("io.mockk:mockk:1.14.12")
            }
        }
    }
}

springBoot {
    buildInfo()
}