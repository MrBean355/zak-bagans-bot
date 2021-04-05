import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    application
}

group = "com.github.mrbean355"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "com.github.mrbean355.zakbot.MainKt"
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("net.dean.jraw:JRAW:1.1.0")
}