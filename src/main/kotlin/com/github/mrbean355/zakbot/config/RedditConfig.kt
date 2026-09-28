package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.RedditUserAgent
import com.github.mrbean355.zakbot.reddit.RedditAuthService
import com.github.mrbean355.zakbot.reddit.RedditLoggingInterceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpStatus
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RedditConfig {

    @Bean
    fun clientHttpRequestFactory(environment: Environment): ClientHttpRequestFactory {
        val baseFactory = JdkClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(15))
        }
        return if (environment.acceptsProfiles(Profiles.of("dev"))) {
            BufferingClientHttpRequestFactory(baseFactory)
        } else {
            baseFactory
        }
    }

    @Bean
    fun restClientBuilder(
        requestFactory: ClientHttpRequestFactory,
        loggingInterceptor: ObjectProvider<RedditLoggingInterceptor>,
    ): RestClient.Builder {
        val builder = RestClient.builder()
            .requestFactory(requestFactory)

        loggingInterceptor.ifAvailable { interceptor ->
            builder.requestInterceptor(interceptor)
        }

        return builder
    }

    @Bean
    fun redditAuthRestClient(builder: RestClient.Builder): RestClient {
        return builder.clone()
            .baseUrl("https://www.reddit.com")
            .defaultHeader("User-Agent", RedditUserAgent)
            .build()
    }

    @Bean
    @Primary
    fun redditRestClient(
        authService: RedditAuthService,
        builder: RestClient.Builder,
    ): RestClient {
        return builder.clone()
            .baseUrl("https://oauth.reddit.com")
            .defaultHeader("User-Agent", RedditUserAgent)
            .requestInterceptor { request, body, execution ->
                request.headers.setBearerAuth(authService.getAccessToken())
                var response = execution.execute(request, body)
                if (response.statusCode == HttpStatus.UNAUTHORIZED) {
                    response.close()
                    authService.invalidateToken()
                    request.headers.setBearerAuth(authService.getAccessToken())
                    response = execution.execute(request, body)
                }
                response
            }
            .build()
    }
}