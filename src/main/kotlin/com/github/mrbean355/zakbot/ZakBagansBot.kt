package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.service.ContributionService
import com.github.mrbean355.zakbot.service.RedditService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val botCache: BotCache,
    private val contributionService: ContributionService,
) {

    @Scheduled(fixedRate = 15 * 60 * 1000L, initialDelay = 10_000L)
    fun checkContributions() {
        redditService.getSubmissionsSince(botCache.getLastSubmissionTime()).apply {
            forEach(contributionService::processSubmission)
            firstOrNull()?.created?.let(botCache::setLastSubmissionTime)
        }

        redditService.getCommentsSince(botCache.getLastCommentTime()).apply {
            forEach(contributionService::processComment)
            firstOrNull()?.created?.let(botCache::setLastCommentTime)
        }

        contributionService.checkDownvotedComments()
    }

    @Scheduled(cron = "@midnight")
    fun updateBotFlair() {
        val options = redditService.getFlairOptions()
        if (options.isNotEmpty()) {
            redditService.setBotFlair(options.random())
        }
    }
}