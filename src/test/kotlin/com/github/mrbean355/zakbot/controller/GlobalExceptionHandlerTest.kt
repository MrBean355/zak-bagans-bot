package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.service.ExceptionNotifier
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.server.ResponseStatusException

class GlobalExceptionHandlerTest {
    @MockK
    private lateinit var exceptionNotifier: ExceptionNotifier

    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        handler = GlobalExceptionHandler(exceptionNotifier)
        every { exceptionNotifier.notify(any(), any()) } returns Unit
    }

    @Test
    fun testHandleValidationException_ReturnsBadRequestWithoutNotifying() {
        val bindingResult = mockk<BindingResult> {
            every { fieldErrors } returns listOf(FieldError("object", "field", "must not be blank"))
        }
        val ex = MethodArgumentNotValidException(mockk(), bindingResult)

        val response = handler.handleValidationException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("must not be blank", response.body?.get("error"))
        verify(inverse = true) { exceptionNotifier.notify(any(), any()) }
    }

    @Test
    fun testHandleResponseStatusException_When4xx_DoesNotNotify() {
        val ex = ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")

        val response = handler.handleResponseStatusException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(inverse = true) { exceptionNotifier.notify(any(), any()) }
    }

    @Test
    fun testHandleResponseStatusException_When5xx_NotifiesTelegram() {
        val ex = ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Downstream failure")

        val response = handler.handleResponseStatusException(ex)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        verify { exceptionNotifier.notify(ex, "HTTP Request") }
    }

    @Test
    fun testHandleUnhandledException_NotifiesTelegramAndReturns500() {
        val ex = IllegalStateException("Unexpected state")

        val response = handler.handleUnhandledException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Internal server error", response.body?.get("error"))
        verify { exceptionNotifier.notify(ex, "HTTP Request") }
    }
}
