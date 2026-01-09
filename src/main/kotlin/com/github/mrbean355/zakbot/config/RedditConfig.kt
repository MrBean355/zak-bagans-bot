package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.AppVersion
import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotClientId
import com.github.mrbean355.zakbot.BotUsername
import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RedditConfig {

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