package com.github.mrbean355.zakbot

import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Comment
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import org.springframework.stereotype.Service

private const val AuthorUsername = "Mr_Bean355"
private const val BotUsername = "ZakBagansBot"
private const val BotClientId = "OiKI0ZAfWG9BBw"
// private const val PostFormat = "%s\n^(I'm a bot! Please report issues to) [^($AuthorUsername)](http://reddit.com/u/$AuthorUsername)^(.)"

@Service
class RedditService {
    private val client = OAuthHelper.automatic(
        OkHttpNetworkAdapter(UserAgent("bot", BotUsername, AppVersion, AuthorUsername)),
        Credentials.script(
            BotUsername,
            System.getenv("BOT_ACCOUNT_PASSWORD"),
            BotClientId,
            System.getenv("BOT_CLIENT_SECRET")
        )
    )

    fun getLatestComments(): List<Comment> =
        client.latestComments("GhostAdventures")
            .build()
            .accumulateMerged(1)
}