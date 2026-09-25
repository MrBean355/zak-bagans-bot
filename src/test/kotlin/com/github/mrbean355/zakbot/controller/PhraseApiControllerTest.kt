package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.service.PhraseService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PhraseApiControllerTest {

    @MockK
    private lateinit var phraseService: PhraseService

    private lateinit var controller: PhraseApiController

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        controller = PhraseApiController(phraseService)
    }

    @Test
    fun testGetAllPhrases_MapsEntitiesToDtos() {
        every { phraseService.getAllPhrases() } returns listOf(
            PhraseEntity(1, "Quote 1", 0, 3, "Source 1"),
            PhraseEntity(2, "Quote 2", 1, 0, null),
        )

        val result = controller.getAllPhrases()

        assertEquals(2, result.size)
        assertEquals(PhraseApiController.PhraseDto(1, "Quote 1", 3, "Source 1"), result[0])
        assertEquals(PhraseApiController.PhraseDto(2, "Quote 2", 0, null), result[1])
    }

    @Test
    fun testAddPhrase_DelegatesToServiceAndReturnsSavedDto() {
        val dto = PhraseApiController.CreatePhraseDto("New quote", 0, "Source")
        every { phraseService.addPhrase("New quote", 0, "Source") } returns PhraseEntity(42, "New quote", 0, 0, "Source")

        val result = controller.addPhrase(dto)

        assertEquals(PhraseApiController.PhraseDto(42, "New quote", 0, "Source"), result)
        verify { phraseService.addPhrase("New quote", 0, "Source") }
    }
}
