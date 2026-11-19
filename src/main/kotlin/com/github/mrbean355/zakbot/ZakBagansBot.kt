package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.service.BotStateService
import com.github.mrbean355.zakbot.service.ContributionService
import com.github.mrbean355.zakbot.reddit.RedditService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val botStateService: BotStateService,
    private val contributionService: ContributionService,
) {

    @Scheduled(fixedRate = 15 * 60 * 1000L, initialDelay = 10_000L)
    fun checkContributions() {
        redditService.getSubmissionsSince(botStateService.getLastSubmissionTime()).apply {
            forEach(contributionService::processSubmission)
            firstOrNull()?.created?.let(botStateService::setLastSubmissionTime)
        }

        redditService.getCommentsSince(botStateService.getLastCommentTime()).apply {
            if (isNotEmpty()) {
                redditService.prefetchInfo(this)
            }
            forEach(contributionService::processComment)
            firstOrNull()?.created?.let(botStateService::setLastCommentTime)
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