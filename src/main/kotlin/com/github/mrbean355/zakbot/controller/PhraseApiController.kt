package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import org.springframework.web.bind.annotation.*

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
    fun addPhrase(@RequestBody phrase: CreatePhraseDto): PhraseDto {
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
        val content: String,
        val type: Int,
        val source: String?
    )
}
