package com.github.mrbean355.zakbot

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.pagination.BarebonesPaginator
import net.dean.jraw.references.CommentReference
import net.dean.jraw.references.SubmissionReference
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RedditServiceTest {
    @RelaxedMockK
    private lateinit var client: RedditClient
    private lateinit var service: RedditService

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        service = RedditService(client)
    }

    @Test
    internal fun testGetSubmissionsSince() {
        service.getSubmissionsSince(mockk())

        verify {
            client.subreddit("GhostAdventures")
                .posts()
                .sorting(SubredditSort.NEW)
                .build()
        }
    }

    @Test
    internal fun testGetCommentsSince() {
        val builder = mockk<BarebonesPaginator.Builder<Comment>> {
            every { build() } returns mockk {
                every { iterator() } returns mockk {
                    every { hasNext() } returns false
                }
            }
        }
        every { client.latestComments(any()) } returns builder

        service.getCommentsSince(mockk())

        verify {
            client.latestComments("GhostAdventures")
            builder.build()
        }
    }

    @Test
    internal fun testReplyToSubmission() {
        val submissionRef = mockk<SubmissionReference> {
            every { reply(any()) } returns mockk()
        }
        every { client.submission(any()) } returns submissionRef
        val submission = mockk<Submission> {
            every { id } returns "123-456"
        }

        service.replyToSubmission(submission, "Credibility...")

        verify {
            client.submission("123-456")
            submissionRef.reply("Credibility...\n\n^(I'm a) [^(bot)](https://github.com/MrBean355/zak-bagans-bot)^(! Please report issues to) [^(/u/Mr_Bean355)](https://www.reddit.com/user/Mr_Bean355)^(.)")
        }
    }

    @Test
    internal fun testReplyToComment() {
        val commentRef = mockk<CommentReference> {
            every { reply(any()) } returns mockk()
        }
        every { client.comment(any()) } returns commentRef
        val comment = mockk<Comment> {
            every { id } returns "123-456"
        }

        service.replyToComment(comment, "We want answers")

        verify {
            client.comment("123-456")
            commentRef.reply("We want answers\n\n^(I'm a) [^(bot)](https://github.com/MrBean355/zak-bagans-bot)^(! Please report issues to) [^(/u/Mr_Bean355)](https://www.reddit.com/user/Mr_Bean355)^(.)")
        }
    }
}