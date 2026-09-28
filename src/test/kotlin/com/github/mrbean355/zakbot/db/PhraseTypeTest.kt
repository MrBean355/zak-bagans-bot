package com.github.mrbean355.zakbot.db

import com.github.mrbean355.zakbot.phrases.AaronPhrase
import com.github.mrbean355.zakbot.phrases.AnswersPhrase
import com.github.mrbean355.zakbot.phrases.FeelingPhrase
import com.github.mrbean355.zakbot.phrases.GenericPhrase
import com.github.mrbean355.zakbot.phrases.MercuryPhrase
import com.github.mrbean355.zakbot.phrases.Phrase
import com.github.mrbean355.zakbot.phrases.SituationPhrase
import com.github.mrbean355.zakbot.phrases.TrinityPhrase
import com.github.mrbean355.zakbot.phrases.UnderstandPhrase
import com.github.mrbean355.zakbot.phrases.ZozoPhrase
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PhraseTypeTest {

    @Test
    internal fun testAllSubclassesCovered() {
        val subclasses = Phrase::class.sealedSubclasses

        assertEquals(9, subclasses.size, "Update phrase type constants!")
    }

    @Test
    fun testConstants() {
        assertEquals(0, PhraseType.Aaron.id)
        assertEquals(1, PhraseType.Answers.id)
        assertEquals(2, PhraseType.Feeling.id)
        assertEquals(3, PhraseType.Generic.id)
        assertEquals(4, PhraseType.Mercury.id)
        assertEquals(5, PhraseType.Situation.id)
        assertEquals(6, PhraseType.Trinity.id)
        assertEquals(7, PhraseType.Understand.id)
        assertEquals(8, PhraseType.Zozo.id)
    }

    @Test
    internal fun testType_MapsToEnum() {
        assertEquals(PhraseType.Aaron, mockk<AaronPhrase>().type())
        assertEquals(PhraseType.Answers, mockk<AnswersPhrase>().type())
        assertEquals(PhraseType.Feeling, mockk<FeelingPhrase>().type())
        assertEquals(PhraseType.Generic, mockk<GenericPhrase>().type())
        assertEquals(PhraseType.Mercury, mockk<MercuryPhrase>().type())
        assertEquals(PhraseType.Situation, mockk<SituationPhrase>().type())
        assertEquals(PhraseType.Trinity, mockk<TrinityPhrase>().type())
        assertEquals(PhraseType.Understand, mockk<UnderstandPhrase>().type())
        assertEquals(PhraseType.Zozo, mockk<ZozoPhrase>().type())
    }

    @Test
    fun testName_MapsToString() {
        assertEquals("Aaron", PhraseType.Aaron.name)
        assertEquals("Answers", PhraseType.Answers.name)
        assertEquals("Feeling", PhraseType.Feeling.name)
        assertEquals("Generic", PhraseType.Generic.name)
        assertEquals("Mercury", PhraseType.Mercury.name)
        assertEquals("Situation", PhraseType.Situation.name)
        assertEquals("Trinity", PhraseType.Trinity.name)
        assertEquals("Understand", PhraseType.Understand.name)
        assertEquals("Zozo", PhraseType.Zozo.name)
        assertEquals("Aaron", PhraseType.name(0))
        assertEquals("Unknown", PhraseType.name(999))
    }

    @Test
    fun testFromJson_ParsesIdsAndNames() {
        assertEquals(PhraseType.Aaron, PhraseType.fromJson(0))
        assertEquals(PhraseType.Aaron, PhraseType.fromJson("0"))
        assertEquals(PhraseType.Aaron, PhraseType.fromJson("Aaron"))
        assertEquals(PhraseType.Aaron, PhraseType.fromJson("aaron"))
        assertEquals(PhraseType.Zozo, PhraseType.fromJson(8))
    }
}