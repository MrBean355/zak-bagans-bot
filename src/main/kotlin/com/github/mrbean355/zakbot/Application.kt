package com.github.mrbean355.zakbot

import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class Application : SpringApplication() {

    @Bean
    open fun provideRedditClient(): RedditClient = OAuthHelper.automatic(
        OkHttpNetworkAdapter(UserAgent("bot", BotUsername, AppVersion, AuthorUsername)),
        Credentials.script(
            BotUsername,
            System.getenv("BOT_ACCOUNT_PASSWORD"),
            BotClientId,
            System.getenv("BOT_CLIENT_SECRET")
        )
    ).apply {
        logHttp = false
    }
}

fun main(vararg args: String) {
    runApplication<Application>(*args)
}