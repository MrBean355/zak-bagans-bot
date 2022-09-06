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
        assertEquals(0, PhraseType.Aaron)
        assertEquals(1, PhraseType.Answers)
        assertEquals(2, PhraseType.Feeling)
        assertEquals(3, PhraseType.Generic)
        assertEquals(4, PhraseType.Mercury)
        assertEquals(5, PhraseType.Situation)
        assertEquals(6, PhraseType.Trinity)
        assertEquals(7, PhraseType.Understand)
        assertEquals(8, PhraseType.Zozo)
    }

    @Test
    internal fun testType_MapsToInt() {
        assertEquals(0, mockk<AaronPhrase>().type())
        assertEquals(1, mockk<AnswersPhrase>().type())
        assertEquals(2, mockk<FeelingPhrase>().type())
        assertEquals(3, mockk<GenericPhrase>().type())
        assertEquals(4, mockk<MercuryPhrase>().type())
        assertEquals(5, mockk<SituationPhrase>().type())
        assertEquals(6, mockk<TrinityPhrase>().type())
        assertEquals(7, mockk<UnderstandPhrase>().type())
        assertEquals(8, mockk<ZozoPhrase>().type())
    }
}