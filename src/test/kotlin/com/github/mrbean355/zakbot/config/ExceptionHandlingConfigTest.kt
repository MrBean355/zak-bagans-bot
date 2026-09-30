package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.service.ExceptionNotifier
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.util.ErrorHandler

class ExceptionHandlingConfigTest {

    @MockK
    private lateinit var exceptionNotifier: ExceptionNotifier

    private lateinit var config: ExceptionHandlingConfig

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        config = ExceptionHandlingConfig()
    }

    @Test
    fun testTaskSchedulerCustomizer_SetsErrorHandlerNotifyingExceptionNotifier() {
        val customizer = config.taskSchedulerCustomizer(exceptionNotifier)
        val scheduler = mockk<ThreadPoolTaskScheduler>(relaxed = true)
        val handlerSlot = slot<ErrorHandler>()
        io.mockk.every { scheduler.setErrorHandler(capture(handlerSlot)) } returns Unit

        customizer.customize(scheduler)

        val exception = RuntimeException("Task failure")
        handlerSlot.captured.handleError(exception)

        verify(exactly = 1) {
            exceptionNotifier.notify(exception, "Scheduled Task")
        }
    }

    @Test
    fun testUncaughtExceptionInitializer_RegistersHandlerThatNotifiesExceptionNotifier() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            val runner = config.uncaughtExceptionInitializer(exceptionNotifier)
            runner.run(mockk())

            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            org.junit.jupiter.api.Assertions.assertNotNull(currentHandler)

            val exception = RuntimeException("Uncaught boom")
            val thread = Thread.currentThread()
            currentHandler?.uncaughtException(thread, exception)

            verify(exactly = 1) {
                exceptionNotifier.notify(exception, "Thread: ${thread.name}")
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }
}
