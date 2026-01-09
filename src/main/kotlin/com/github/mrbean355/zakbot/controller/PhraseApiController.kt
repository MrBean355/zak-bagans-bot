package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/phrases")
class PhraseApiController(
    private val phraseRepository: PhraseRepository
) {

    @GetMapping
    fun getAllPhrases(): List<PhraseDto> {
        return phraseRepository.findAll().map {
            PhraseDto(it.id, it.content, it.type, it.source)
        }
    }

    @PostMapping
    fun addPhrase(@RequestBody @Valid phrase: CreatePhraseDto): PhraseDto {
        val minUsages = phraseRepository.findMinUsagesByType(phrase.type) ?: 0
        val entity = PhraseEntity(0, phrase.content, minUsages, phrase.type, phrase.source)
        val saved = phraseRepository.save(entity)
        return PhraseDto(saved.id, saved.content, saved.type, saved.source)
    }

    @DeleteMapping("/{id}")
    fun deletePhrase(@PathVariable id: Int) {
        phraseRepository.deleteById(id)
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
