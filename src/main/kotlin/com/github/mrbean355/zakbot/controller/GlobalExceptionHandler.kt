package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.service.ExceptionNotifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val exceptionNotifier: ExceptionNotifier
) {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to message))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, String>> {
        if (ex.statusCode.is5xxServerError) {
            exceptionNotifier.notify(ex, "HTTP Request")
        }
        return ResponseEntity.status(ex.statusCode)
            .body(mapOf("error" to (ex.reason ?: "Request error")))
    }

    @ExceptionHandler(Throwable::class)
    fun handleUnhandledException(throwable: Throwable): ResponseEntity<Map<String, String>> {
        exceptionNotifier.notify(throwable, "HTTP Request")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal server error"))
    }
}
