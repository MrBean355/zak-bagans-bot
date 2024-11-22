package com.github.mrbean355.zakbot

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Listing
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.pagination.BarebonesPaginator
import net.dean.jraw.references.CommentReference
import net.dean.jraw.references.SelfUserFlairReference
import net.dean.jraw.references.SubmissionReference
import org.junit.jupiter.api.Assertions.assertNull
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
        service = RedditService(client, true)
    }

    @Test
    internal fun testGetSubmissionsSince() {
        service.getSubmissionsSince(mockk())

        verify {
            client.subreddit("GhostAdventures")
                .posts()
                .sorting(SubredditSort.NEW)
                .limit(5)
                .build()
        }
        confirmVerified(client)
    }

    @Test
    internal fun testGetCommentsSince() {
        val builder = mockk<BarebonesPaginator.Builder<Comment>> {
            every { limit(any()) } returns this
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
            builder.limit(5)
            builder.build()
        }
        confirmVerified(client, builder)
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
            submissionRef.reply("Credibility...")
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
            commentRef.reply("We want answers")
        }
    }

    @Test
    internal fun testGetCommentSubmission_CallsApiCorrectly() {
        val comment = mockk<Comment> {
            every { submissionFullName } returns "t1_6afe8u"
        }

        service.getCommentSubmission(comment)

        verify {
            client.lookup("t1_6afe8u")
        }
    }

    @Test
    internal fun testGetCommentSubmission_ApiReturnsEmptyList_ReturnsNull() {
        val comment = mockk<Comment> {
            every { submissionFullName } returns "t1_6afe8u"
        }
        every { client.lookup(*anyVararg()) } returns Listing.empty()

        val result = service.getCommentSubmission(comment)

        assertNull(result)
    }

    @Test
    internal fun testGetCommentSubmission_ApiReturnsNonEmptyList_FirstItemIsNotSubmission_ReturnsNull() {
        val comment = mockk<Comment> {
            every { submissionFullName } returns "t1_6afe8u"
        }
        every { client.lookup(*anyVararg()) } returns Listing.create(null, listOf(mockk<Comment>()))

        val result = service.getCommentSubmission(comment)

        assertNull(result)
    }

    @Test
    internal fun testGetCommentSubmission_ApiReturnsNonEmptyList_FirstItemIsComment_ReturnsComment() {
        val comment = mockk<Comment> {
            every { submissionFullName } returns "t1_6afe8u"
        }
        val parent = mockk<Submission>()
        every { client.lookup(*anyVararg()) } returns Listing.create(null, listOf(parent))

        val result = service.getCommentSubmission(comment)

        assertSame(parent, result)
    }

    @Test
    internal fun testFindParentComment_CallsApiCorrectly() {
        val comment = mockk<Comment> {
            every { parentFullName } returns "t1_6afe8u"
        }

        service.findParentComment(comment)

        verify {
            client.lookup("t1_6afe8u")
        }
    }

    @Test
    internal fun testFindParentComment_ApiReturnsEmptyList_ReturnsNull() {
        val comment = mockk<Comment> {
            every { parentFullName } returns "t1_6afe8u"
        }
        every { client.lookup(*anyVararg()) } returns Listing.empty()

        val result = service.findParentComment(comment)

        assertNull(result)
    }

    @Test
    internal fun testFindParentComment_ApiReturnsNonEmptyList_FirstItemIsNotComment_ReturnsNull() {
        val comment = mockk<Comment> {
            every { parentFullName } returns "t1_6afe8u"
        }
        every { client.lookup(*anyVararg()) } returns Listing.create(null, listOf(mockk<Submission>()))

        val result = service.findParentComment(comment)

        assertNull(result)
    }

    @Test
    internal fun testFindParentComment_ApiReturnsNonEmptyList_FirstItemIsComment_ReturnsComment() {
        val comment = mockk<Comment> {
            every { parentFullName } returns "t1_6afe8u"
        }
        val parent = mockk<Comment>()
        every { client.lookup(*anyVararg()) } returns Listing.create(null, listOf(parent))

        val result = service.findParentComment(comment)

        assertSame(parent, result)
    }

    @Test
    internal fun testGetFlairOptions_ReturnsApiResult() {
        val options = mockk<List<Flair>>()
        every { client.subreddit("GhostAdventures") } returns mockk {
            every { userFlairOptions() } returns options
        }

        val result = service.getFlairOptions()

        assertSame(options, result)
    }

    @Test
    internal fun testSetBotFlair_InvokesApi() {
        val selfFlair = mockk<SelfUserFlairReference> {
            justRun { updateToTemplate(any(), any()) }
        }
        every { client.subreddit("GhostAdventures") } returns mockk {
            every { selfUserFlair() } returns selfFlair
        }
        val flairChoice = mockk<Flair> {
            every { id } returns "flair-id"
            every { text } returns "flair-text"
        }

        service.setBotFlair(flairChoice)

        verify { selfFlair.updateToTemplate("flair-id", "flair-text") }
    }
}