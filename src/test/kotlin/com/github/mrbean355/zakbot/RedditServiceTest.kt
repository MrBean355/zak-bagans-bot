package com.github.mrbean355.zakbot

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import net.dean.jraw.pagination.BarebonesPaginator
import net.dean.jraw.references.CommentReference
import org.junit.jupiter.api.Assertions.assertSame
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
    internal fun testGetLatestComments() {
        val comments = listOf<Comment>()
        val paginator = mockk<BarebonesPaginator<Comment>> {
            every { accumulateMerged(any()) } returns comments
        }
        val builder = mockk<BarebonesPaginator.Builder<Comment>> {
            every { build() } returns paginator
        }
        every { client.latestComments(any()) } returns builder

        val actual = service.getLatestComments()

        assertSame(comments, actual)
        verify {
            client.latestComments("GhostAdventures")
            builder.build()
            paginator.accumulateMerged(1)
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