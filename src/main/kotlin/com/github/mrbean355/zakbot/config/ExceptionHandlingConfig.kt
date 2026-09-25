package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.service.ExceptionNotifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExceptionHandlingConfig {

    @Bean
    fun taskSchedulerCustomizer(exceptionNotifier: ExceptionNotifier) =
        ThreadPoolTaskSchedulerCustomizer { scheduler ->
            scheduler.setErrorHandler { throwable ->
                exceptionNotifier.notify(throwable, "Scheduled Task")
            }
        }

    @Bean
    fun uncaughtExceptionInitializer(exceptionNotifier: ExceptionNotifier) =
        ApplicationRunner {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                exceptionNotifier.notify(throwable, "Thread: ${thread.name}")
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
}
