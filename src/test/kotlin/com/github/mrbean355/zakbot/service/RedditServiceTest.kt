package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.SubredditName
import com.github.mrbean355.zakbot.config.ZakbotProperties
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.RedditFlair
import com.github.mrbean355.zakbot.reddit.model.Submission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant

class RedditServiceTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var serviceWithReplies: RedditService
    private lateinit var serviceWithoutReplies: RedditService

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("https://oauth.reddit.com")
        server = MockRestServiceServer.bindTo(builder).bufferContent().build()
        val client = builder.build()

        serviceWithReplies = RedditService(client, ZakbotProperties(replies = ZakbotProperties.Replies(enabled = true)))
        serviceWithoutReplies = RedditService(client, ZakbotProperties(replies = ZakbotProperties.Replies(enabled = false)))
    }

    @Test
    fun testGetSubmissionsSince_FiltersByDate() {
        val json = """
            {
              "kind": "Listing",
              "data": {
                "after": null,
                "children": [
                  {
                    "kind": "t3",
                    "data": {
                      "id": "sub1",
                      "name": "t3_sub1",
                      "author": "user1",
                      "title": "Title 1",
                      "selftext": "Text 1",
                      "created_utc": 1700000050.0,
                      "permalink": "/r/GhostAdventures/comments/sub1/title_1/",
                      "url": "https://reddit.com/r/GhostAdventures/comments/sub1/title_1/"
                    }
                  },
                  {
                    "kind": "t3",
                    "data": {
                      "id": "sub2",
                      "name": "t3_sub2",
                      "author": "user2",
                      "title": "Title 2",
                      "selftext": "Text 2",
                      "created_utc": 1700000000.0,
                      "permalink": "/r/GhostAdventures/comments/sub2/title_2/",
                      "url": null
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        server.expect(requestTo("https://oauth.reddit.com/r/$SubredditName/new?limit=5"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val results = serviceWithReplies.getSubmissionsSince(Instant.ofEpochMilli(1700000010_000L))

        assertEquals(1, results.size)
        assertEquals("sub1", results[0].id)
        assertEquals("t3_sub1", results[0].fullName)
        assertEquals("user1", results[0].author)
        assertEquals("Title 1", results[0].title)
        assertEquals(Instant.ofEpochMilli(1700000050_000L), results[0].created)
        assertEquals("https://www.reddit.com/r/GhostAdventures/comments/sub1/title_1/", results[0].url)
        server.verify()
    }

    @Test
    fun testGetCommentsSince_FiltersByDate() {
        val json = """
            {
              "kind": "Listing",
              "data": {
                "after": null,
                "children": [
                  {
                    "kind": "t1",
                    "data": {
                      "id": "com1",
                      "name": "t1_com1",
                      "author": "user1",
                      "body": "Body 1",
                      "link_id": "t3_post1",
                      "parent_id": "t1_parent1",
                      "created_utc": 1700000050.0,
                      "permalink": "/r/GhostAdventures/comments/post1/title/com1/"
                    }
                  },
                  {
                    "kind": "t1",
                    "data": {
                      "id": "com2",
                      "name": "t1_com2",
                      "author": "user2",
                      "body": "Body 2",
                      "link_id": "t3_post1",
                      "parent_id": "t3_post1",
                      "created_utc": 1700000000.0,
                      "permalink": "/r/GhostAdventures/comments/post1/title/com2/"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        server.expect(requestTo("https://oauth.reddit.com/r/$SubredditName/comments?limit=5"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val results = serviceWithReplies.getCommentsSince(Instant.ofEpochMilli(1700000010_000L))

        assertEquals(1, results.size)
        assertEquals("com1", results[0].id)
        assertEquals("t1_com1", results[0].fullName)
        assertEquals("user1", results[0].author)
        assertEquals("Body 1", results[0].body)
        assertEquals("t3_post1", results[0].submissionFullName)
        assertEquals("t1_parent1", results[0].parentFullName)
        server.verify()
    }

    @Test
    fun testReplyToSubmission_WhenRepliesEnabled_SendsPostRequest() {
        server.expect(requestTo("https://oauth.reddit.com/api/comment"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formDataContains(mapOf("api_type" to "json", "thing_id" to "t3_sub1", "text" to "Credibility...")))
            .andRespond(withSuccess())

        val submission = Submission("sub1", "t3_sub1", "author", Instant.now(), "url", "Title", null)
        serviceWithReplies.replyToSubmission(submission, "Credibility...")

        server.verify()
    }

    @Test
    fun testReplyToSubmission_WhenRepliesDisabled_DoesNotSendRequest() {
        val submission = Submission("sub1", "t3_sub1", "author", Instant.now(), "url", "Title", null)
        serviceWithoutReplies.replyToSubmission(submission, "Credibility...")

        server.verify()
    }

    @Test
    fun testReplyToComment_WhenRepliesEnabled_SendsPostRequest() {
        server.expect(requestTo("https://oauth.reddit.com/api/comment"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formDataContains(mapOf("api_type" to "json", "thing_id" to "t1_com1", "text" to "We want answers")))
            .andRespond(withSuccess())

        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_post1", "t1_parent")
        serviceWithReplies.replyToComment(comment, "We want answers")

        server.verify()
    }

    @Test
    fun testReplyToComment_WhenRepliesDisabled_DoesNotSendRequest() {
        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_post1", "t1_parent")
        serviceWithoutReplies.replyToComment(comment, "We want answers")

        server.verify()
    }

    @Test
    fun testGetCommentSubmission_WhenFound_ReturnsSubmission() {
        val json = """
            {
              "kind": "Listing",
              "data": {
                "children": [
                  {
                    "kind": "t3",
                    "data": {
                      "id": "sub1",
                      "name": "t3_sub1",
                      "author": "author1",
                      "title": "Post Title",
                      "selftext": "Self text",
                      "created_utc": 1700000000.0,
                      "permalink": "/r/GhostAdventures/comments/sub1/",
                      "url": "https://reddit.com"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        server.expect(requestTo("https://oauth.reddit.com/api/info?id=t3_sub1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_sub1", "t1_parent")
        val result = serviceWithReplies.getCommentSubmission(comment)

        assertNotNull(result)
        assertEquals("sub1", result?.id)
        assertEquals("Post Title", result?.title)
        server.verify()
    }

    @Test
    fun testGetCommentSubmission_WhenNotFound_ReturnsNull() {
        val json = """{ "kind": "Listing", "data": { "children": [] } }"""

        server.expect(requestTo("https://oauth.reddit.com/api/info?id=t3_unknown"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_unknown", "t1_parent")
        val result = serviceWithReplies.getCommentSubmission(comment)

        assertNull(result)
        server.verify()
    }

    @Test
    fun testFindParentComment_WhenParentIsPost_ReturnsNullWithoutCallingApi() {
        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_sub1", "t3_sub1")
        val result = serviceWithReplies.findParentComment(comment)

        assertNull(result)
        server.verify()
    }

    @Test
    fun testFindParentComment_WhenParentIsComment_ReturnsComment() {
        val json = """
            {
              "kind": "Listing",
              "data": {
                "children": [
                  {
                    "kind": "t1",
                    "data": {
                      "id": "parent1",
                      "name": "t1_parent1",
                      "author": "parent_author",
                      "body": "Parent body",
                      "link_id": "t3_sub1",
                      "parent_id": "t3_sub1",
                      "created_utc": 1700000000.0,
                      "permalink": "/r/GhostAdventures/comments/sub1/title/parent1/"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        server.expect(requestTo("https://oauth.reddit.com/api/info?id=t1_parent1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val comment = Comment("com1", "t1_com1", "author", Instant.now(), "url", "body", "t3_sub1", "t1_parent1")
        val result = serviceWithReplies.findParentComment(comment)

        assertNotNull(result)
        assertEquals("parent1", result?.id)
        assertEquals("Parent body", result?.body)
        server.verify()
    }

    @Test
    fun testUserExists_WhenUserFoundAndNotSuspended_ReturnsTrue() {
        val json = """{ "data": { "is_suspended": false } }"""

        server.expect(requestTo("https://oauth.reddit.com/user/active_user/about"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        assertTrue(serviceWithReplies.userExists("active_user"))
        server.verify()
    }

    @Test
    fun testUserExists_WhenUserSuspended_ReturnsFalse() {
        val json = """{ "data": { "is_suspended": true } }"""

        server.expect(requestTo("https://oauth.reddit.com/user/banned_user/about"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        assertFalse(serviceWithReplies.userExists("banned_user"))
        server.verify()
    }

    @Test
    fun testUserExists_WhenUserNotFound_ReturnsFalse() {
        server.expect(requestTo("https://oauth.reddit.com/user/ghost_user/about"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertFalse(serviceWithReplies.userExists("ghost_user"))
        server.verify()
    }

    @Test
    fun testUserExists_WhenDataIsNull_ReturnsFalse() {
        val json = """{ "data": null }"""

        server.expect(requestTo("https://oauth.reddit.com/user/shadow_user/about"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        assertFalse(serviceWithReplies.userExists("shadow_user"))
        server.verify()
    }

    @Test
    fun testGetFlairOptions_ReturnsMappedFlairs() {
        val json = """
            [
              { "id": "flair-1", "text": "Flair One" },
              { "id": "flair-2", "text": "Flair Two" }
            ]
        """.trimIndent()

        server.expect(requestTo("https://oauth.reddit.com/r/$SubredditName/api/user_flair_v2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val result = serviceWithReplies.getFlairOptions()

        assertEquals(2, result.size)
        assertEquals("flair-1", result[0].id)
        assertEquals("Flair One", result[0].text)
        server.verify()
    }

    @Test
    fun testSetBotFlair_SendsSelectFlairRequest() {
        server.expect(requestTo("https://oauth.reddit.com/r/$SubredditName/api/selectflair"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formDataContains(mapOf(
                "api_type" to "json",
                "name" to "ZakBagansBot",
                "flair_template_id" to "flair-1",
                "text" to "Flair One"
            )))
            .andRespond(withSuccess())

        serviceWithReplies.setBotFlair(RedditFlair("flair-1", "Flair One"))

        server.verify()
    }
}