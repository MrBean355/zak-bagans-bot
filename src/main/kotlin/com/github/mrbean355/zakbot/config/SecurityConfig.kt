package com.github.mrbean355.zakbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
open class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/phrases").permitAll()
                    .requestMatchers("/api/**", "/admin.html").authenticated()
                    .anyRequest().permitAll()
            }
            .httpBasic(Customizer.withDefaults())
            .csrf { it.disable() }

        return http.build()
    }
}
