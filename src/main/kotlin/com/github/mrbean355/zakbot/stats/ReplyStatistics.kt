package com.github.mrbean355.zakbot.stats

import com.github.mrbean355.zakbot.RedditService
import com.github.mrbean355.zakbot.SubredditName
import org.springframework.stereotype.Component
import java.io.File

@Component
class ReplyStatistics(
    private val redditService: RedditService
) {

    fun fetchReplyStatistics() {
        val scoredComments = mutableListOf<ScoredReply>()

        redditService.getAllBotComments().forEach { listing ->
            listing.filter { it.subreddit == SubredditName }.forEach { contribution ->
                contribution.body.orEmpty()
                    .removeSuffix(OldSuffix)
                    .unsubstitute()
                    .let { ReplacedResponses[it] ?: it }
                    .takeIf { it !in IgnoredResponses }
                    ?.also { scoredComments += ScoredReply(it, contribution.score) }
            }
        }

        val text = buildString {
            scoredComments.sortedBy { it.reply.lowercase() }
                .groupBy { it.reply }
                .forEach { (body, comments) ->
                    val totalComments = comments.size
                    val totalScore = comments.sumOf { it.score }
                    append(body).append('|')
                        .append(comments.size).append('|')
                        .append(totalScore).append('|')
                        .append(totalScore.toDouble() / totalComments).appendLine()
                }
        }

        File("stats.csv").writeText(text)
    }

    private fun String.unsubstitute(): String {
        val match = Substitutions.firstNotNullOfOrNull { it.matchEntire(this) }
        return if (match != null) {
            replace(match.groupValues[1], "{author}")
        } else {
            this
        }
    }

    private data class ScoredReply(
        val reply: String,
        val score: Int
    )

    private companion object {
        /** Remove this suffix from responses. */
        private const val OldSuffix = "\n\n^(I'm a) [^(bot)](https://github.com/MrBean355/zak-bagans-bot)^(! Please report issues to) [^(/u/Mr_Bean355)](https://www.reddit.com/user/Mr_Bean355)^(.)"

        /** Remove substitutions from responses. */
        private val Substitutions = listOf(
            "A light anomaly just shot into (.*)'s head!".toRegex(),
            "A light orb just shot into (.*)'s arm!".toRegex(),
            "A ball of light just shot into (.*)'s chest!".toRegex(),
            "(.*) is mocking the Holy Trinity.".toRegex(),
            "(.*) mentioned the number '3'. They must be mocking the Trinity.".toRegex(),
        )

        /** Replace these responses with new ones, usually for spelling/grammar mistakes. */
        private val ReplacedResponses = mapOf(
            "Not only did we come back, but we came… with a female!" to "Not only did we come back, but we came... with a female!",
            "I just felt something grab my ass...Like, hard, Nick! Hard, I could feel like this on my-on my BUTT!" to "I just felt something grab my ass... like, hard, Nick! Hard, I could feel like this on my-on my BUTT!",
            "For the investigation, we need to find women, put masks on them, and and,uhhhh, have us recreate these orgies." to "For the investigation, we need to find women, put masks on them, and and, uhhhh, have us recreate these orgies.",
            "I think our human eyeballs, our living eyeballs see this town as it is now." to "I think our human eyeballs - our living eyeballs - see this town as it is now.",
        )

        /** Ignore these responses, usually ones that were sent manually. */
        private val IgnoredResponses = listOf(
            "<3",
            "\uD83D\uDC9C", // 💜
            "\uD83D\uDE22", // 😢
            "Sorry :(",
            "U serious bruh?!",
            "u/Mr_Bean355"
        )
    }
}