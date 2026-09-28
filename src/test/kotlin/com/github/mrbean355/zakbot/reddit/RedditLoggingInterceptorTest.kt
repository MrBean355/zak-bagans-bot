package com.github.mrbean355.zakbot.reddit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class RedditLoggingInterceptorTest {

    @Test
    fun testInterceptor_PassesThroughSuccessfulRequestAndResponse() {
        val builder = RestClient.builder()
            .requestFactory(BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory()))
            .requestInterceptor(RedditLoggingInterceptor())

        val server = MockRestServiceServer.bindTo(builder).bufferContent().build()
        val client = builder.build()

        server.expect(requestTo("/test"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"result": "ok"}""", MediaType.APPLICATION_JSON))

        val result = client.post()
            .uri("/test")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=password&username=bot&password=secretpassword")
            .retrieve()
            .body(String::class.java)

        assertEquals("""{"result": "ok"}""", result)
        server.verify()
    }

    @Test
    fun testInterceptor_PassesThroughErrorResponse() {
        val builder = RestClient.builder()
            .requestFactory(BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory()))
            .requestInterceptor(RedditLoggingInterceptor())

        val server = MockRestServiceServer.bindTo(builder).bufferContent().build()
        val client = builder.build()

        server.expect(requestTo("/test-error"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("""{"error": 400}"""))

        try {
            client.get().uri("/test-error").retrieve().toBodilessEntity()
        } catch (_: Exception) {
            // Expected
        }

        server.verify()
    }
}
