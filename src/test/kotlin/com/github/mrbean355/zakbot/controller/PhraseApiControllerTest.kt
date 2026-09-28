package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.db.PhraseType
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
            PhraseEntity(1, "Quote 1", 0, PhraseType.Generic, "Source 1"),
            PhraseEntity(2, "Quote 2", 1, PhraseType.Aaron, null),
        )

        val result = controller.getAllPhrases()

        assertEquals(2, result.size)
        assertEquals(PhraseApiController.PhraseDto(1, "Quote 1", PhraseType.Generic, "Source 1"), result[0])
        assertEquals(PhraseApiController.PhraseDto(2, "Quote 2", PhraseType.Aaron, null), result[1])
    }

    @Test
    fun testAddPhrase_DelegatesToServiceAndReturnsSavedDto() {
        val dto = PhraseApiController.CreatePhraseDto("New quote", PhraseType.Aaron, "Source")
        every { phraseService.addPhrase("New quote", PhraseType.Aaron, "Source") } returns PhraseEntity(
            42,
            "New quote",
            0,
            PhraseType.Aaron,
            "Source"
        )

        val result = controller.addPhrase(dto)

        assertEquals(PhraseApiController.PhraseDto(42, "New quote", PhraseType.Aaron, "Source"), result)
        verify { phraseService.addPhrase("New quote", PhraseType.Aaron, "Source") }
    }
}
