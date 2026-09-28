package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.phrases.GenericPhrase
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Submission
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class PhraseServiceTest {
    @MockK
    private lateinit var phraseRepository: PhraseRepository

    @MockK
    private lateinit var genericPhrase: GenericPhrase

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var service: PhraseService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { genericPhrase.priority } returns 1
        every { genericPhrase.getReplyChance(any()) } returns 0f
        every { genericPhrase.getReplyChance("hello zak") } returns 1f

        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(
            PhraseEntity(1, "This is a generic quote.", 5, PhraseType.Generic, "Unit tests"),
            PhraseEntity(2, "This is another one.", 4, PhraseType.Generic, "Unit tests"),
        )
        every { phraseRepository.save<PhraseEntity>(any()) } answers { firstArg() }

        service = PhraseService(phraseRepository, listOf(genericPhrase), telegramNotifier)
    }

    @Test
    fun testFindPhrase_ForComment_ConvertsToLowercase() {
        val comment = createComment("Hello Zak")

        service.findPhrase(comment)

        verify { genericPhrase.getReplyChance("hello zak") }
    }

    @Test
    fun testFindPhrase_ForComment_ExcludesUrlsFromText() {
        val comment = createComment("Hello www.Zak.com")

        service.findPhrase(comment)

        verify { genericPhrase.getReplyChance("hello ") }
    }

    @Test
    fun testFindPhrase_ForComment_ChanceNotMet_ReturnsNull() {
        val comment = createComment("Aaron")

        val actual = service.findPhrase(comment)

        assertNull(actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_ReturnsPhraseWithLeastUsages() {
        val comment = createComment("Hello Zak")

        val actual = service.findPhrase(comment)

        assertEquals("This is another one.\n\n^(Quote from: Unit tests)", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_SavesIncrementedUsageCount() {
        val comment = createComment("Hello Zak")

        service.findPhrase(comment)

        verify { phraseRepository.save(PhraseEntity(2, "This is another one.", 5, PhraseType.Generic, "Unit tests")) }
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithSource_ReturnsPhraseWithSource() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = "Unit tests"))
        val comment = createComment("Hello Zak")

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.\n\n^(Quote from: Unit tests)", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithSource_ReturnsSourceWithEscapedParentheses() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = "Unit tests (House Calls)"))
        val comment = createComment("Hello Zak")

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.\n\n^(Quote from: Unit tests \\(House Calls\\))", actual)
    }

    @Test
    fun testFindPhrase_ForComment_MatchingText_QuoteWithoutSource_ReturnsPhraseWithoutSource() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns listOf(PhraseEntity(1, "This is a generic quote.", 0, PhraseType.Generic, source = null))
        val comment = createComment("Hello Zak")

        val actual = service.findPhrase(comment)

        assertEquals("This is a generic quote.", actual)
    }

    @Test
    fun testFindPhrase_WhenNoChoicesInDatabase_ReturnsNull() {
        every { phraseRepository.findByType(PhraseType.Generic) } returns emptyList()
        val comment = createComment("Hello Zak")

        val actual = service.findPhrase(comment)

        assertNull(actual)
    }

    @Test
    fun testFindPhrase_ForSubmission_ChecksTitleAndBody() {
        val submission = createSubmission("Title", "Hello Zak")

        service.findPhrase(submission)

        verify {
            genericPhrase.getReplyChance("title")
            genericPhrase.getReplyChance("hello zak")
        }
    }

    @Test
    fun testGetAllPhrases_ReturnsAllPhrasesFromRepository() {
        val expected = listOf(
            PhraseEntity(1, "Quote 1", 2, PhraseType.Generic, "Source 1"),
            PhraseEntity(2, "Quote 2", 3, PhraseType.Aaron, "Source 2"),
        )
        every { phraseRepository.findAll() } returns expected

        val actual = service.getAllPhrases()

        assertEquals(expected, actual)
    }

    @Test
    fun testAddPhrase_CalculatesMinUsagesAndSavesEntity() {
        every { phraseRepository.findMinUsagesByType(PhraseType.Aaron) } returns 4
        every { phraseRepository.save(any<PhraseEntity>()) } answers { firstArg() }

        val actual = service.addPhrase("New phrase", PhraseType.Aaron, "Source")

        assertEquals(0, actual.id)
        assertEquals("New phrase", actual.content)
        assertEquals(4, actual.usages)
        assertEquals(PhraseType.Aaron, actual.type)
        assertEquals("Source", actual.source)
        verify {
            phraseRepository.save(PhraseEntity(0, "New phrase", 4, PhraseType.Aaron, "Source"))
            telegramNotifier.sendMessage(match { it.contains("New phrase") && it.contains("Aaron") && it.contains("Source") })
        }
    }

    @Test
    fun testAddPhrase_WhenNoMinUsagesFound_DefaultsToZero() {
        every { phraseRepository.findMinUsagesByType(PhraseType.Zozo) } returns null
        every { phraseRepository.save(any<PhraseEntity>()) } answers { firstArg() }

        val actual = service.addPhrase("Zozo phrase", PhraseType.Zozo, null)

        assertEquals(0, actual.usages)
        verify {
            phraseRepository.save(PhraseEntity(0, "Zozo phrase", 0, PhraseType.Zozo, null))
            telegramNotifier.sendMessage(match { it.contains("Zozo phrase") && it.contains("Zozo") && it.contains("None") })
        }
    }

    private fun createComment(body: String) = Comment(
        id = "1",
        fullName = "t1_1",
        author = "tester",
        created = Date(),
        url = "https://reddit.com",
        body = body,
        submissionFullName = "t3_1",
        parentFullName = "t1_parent",
    )

    private fun createSubmission(title: String, selfText: String?) = Submission(
        id = "1",
        fullName = "t3_1",
        author = "tester",
        created = Date(),
        url = "https://reddit.com",
        title = title,
        selfText = selfText,
    )
}