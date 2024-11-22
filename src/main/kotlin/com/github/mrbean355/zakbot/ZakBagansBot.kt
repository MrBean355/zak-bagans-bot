package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.service.ContributionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val botCache: BotCache,
    private val contributionService: ContributionService,
) {

    @Scheduled(fixedRate = 15 * 60 * 1000L)
    fun checkContributions() {
        redditService.getSubmissionsSince(botCache.getLastPostTime()).apply {
            firstOrNull()?.created?.let(botCache::setLastPostTime)
            forEach(contributionService::processSubmission)
        }

        redditService.getCommentsSince(botCache.getLastCommentTime()).apply {
            firstOrNull()?.created?.let(botCache::setLastCommentTime)
            forEach(contributionService::processComment)
        }
    }

    @Scheduled(cron = "@midnight")
    fun updateBotFlair() {
        val options = redditService.getFlairOptions()
        if (options.isNotEmpty()) {
            redditService.setBotFlair(options.random())
        }
    }
}