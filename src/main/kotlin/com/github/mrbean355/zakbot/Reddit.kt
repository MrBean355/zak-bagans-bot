package com.github.mrbean355.zakbot

import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Comment
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper

private const val AuthorUsername = "Mr_Bean355"
const val BotUsername = "ZakBagansBot"
private const val BotClientId = "OiKI0ZAfWG9BBw"
private const val PostFormat = "%s\n^(I'm a bot! Please report issues to) [^($AuthorUsername)](http://reddit.com/u/$AuthorUsername)^(.)"

private val Agent = UserAgent("bot", BotUsername, "0.0.1", AuthorUsername)
private val AppCredentials = Credentials.script(
    BotUsername,
    System.getenv("BOT_ACCOUNT_PASSWORD"),
    BotClientId,
    System.getenv("BOT_CLIENT_SECRET")
)
private val client = OAuthHelper.automatic(OkHttpNetworkAdapter(Agent), AppCredentials)

fun getRecentComments(): List<Comment> =
    client.latestComments("GhostAdventures")
        .build()
        .accumulateMerged(1)