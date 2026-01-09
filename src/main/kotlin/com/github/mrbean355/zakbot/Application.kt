package com.github.mrbean355.zakbot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class Application : SpringApplication()

fun main(vararg args: String) {
    runApplication<Application>(*args)
}