package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.phrases.GenericPhrase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PhraseServiceTest {
    @MockK
    private lateinit var phraseRepository: PhraseRepository

    @MockK
    private lateinit var genericPhrase: GenericPhrase
    private lateinit var service: PhraseService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        every { genericPhrase.priority } returns 1
        every { genericPhrase.getReplyChance(any()) } returns 0f
        every { genericPhrase.getReplyChance("hello zak") } returns 1f

        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(
            PhraseEntity(1, "This is a generic quote.", 5, PhraseType.Generic, "Unit tests"),
            PhraseEntity(2, "This is another one.", 4, PhraseType.Generic, "Unit tests"),
        )
        every { phraseRepository.save<PhraseEntity>(any()) } answers { firstArg() }

        service = PhraseService(phraseRepository, listOf(genericPhrase))
    }

    @Test
    fun testFindPhrase_ForComment_ConvertsToLowercase() {
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        service.findPhrase(comment)

        verify { genericPhrase.getReplyChance("hello zak") }
    }

    @Test
    fun testFindPhrase_ForComment_ExcludesUrlsFromText() {
        val comment = mockk<Comment> {
            every { body } returns "Hello www.Zak.com"
        }

        service.findPhrase(comment)

        verify { genericPhrase.getReplyChance("hello ") }
    }

    @Test
    fun testFindPhrase_ForComment_ChanceNotMet_ReturnsNull() {
        val comment = mockk<Comment> {
            every { body } returns "Aaron"
        }

        val actual = service.findPhrase(comment)

        assertNull(actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_ReturnsPhraseWithLeastUsages() {
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        val actual = service.findPhrase(comment)

        assertEquals("This is another one.\n\n^(Quote from: Unit tests)", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_SavesIncrementedUsageCount() {
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        service.findPhrase(comment)

        verify { phraseRepository.save(PhraseEntity(2, "This is another one.", 5, PhraseType.Generic, "Unit tests")) }
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithSource_ReturnsPhraseWithSource() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = "Unit tests"))
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.\n\n^(Quote from: Unit tests)", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithSource_ReturnsSourceWithEscapedParentheses() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = "Unit tests (House Calls)"))
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.\n\n^(Quote from: Unit tests \\(House Calls\\))", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithoutSource_ReturnsPhraseWithoutSource() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = null))
        val comment = mockk<Comment> {
            every { body } returns "Hello Zak"
        }

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.", actual)
    }

    @Test
    fun testFindPhrase_ForSubmission_ChecksTitleAndBody() {
        val submission = mockk<Submission> {
            every { title } returns "Title"
            every { selfText } returns "Hello Zak"
        }

        service.findPhrase(submission)

        verify {
            genericPhrase.getReplyChance("title")
            genericPhrase.getReplyChance("hello zak")
        }
    }
}