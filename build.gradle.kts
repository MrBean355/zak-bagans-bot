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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.telegram:telegrambots:5.0.1.1")
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "com.github.mrbean355.zakbot.MainKt")
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}