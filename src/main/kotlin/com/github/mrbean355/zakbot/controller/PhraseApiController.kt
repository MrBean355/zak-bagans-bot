package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.service.PhraseService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/phrases")
class PhraseApiController(
    private val phraseService: PhraseService
) {

    @GetMapping
    fun getAllPhrases(): List<PhraseDto> {
        return phraseService.getAllPhrases().map {
            PhraseDto(it.id, it.content, it.type, it.source)
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addPhrase(@RequestBody @Valid phrase: CreatePhraseDto): PhraseDto {
        val saved = phraseService.addPhrase(phrase.content, phrase.type, phrase.source)
        return PhraseDto(saved.id, saved.content, saved.type, saved.source)
    }

    data class PhraseDto(
        val id: Int,
        val content: String,
        val type: Int,
        val source: String?
    )

    data class CreatePhraseDto(
        @field:NotBlank
        @field:Size(max = 255)
        val content: String,
        @field:Min(0)
        @field:Max(8)
        val type: Int,
        @field:Size(max = 255)
        val source: String?
    )
}
