package com.github.mrbean355.zakbot.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PhraseTypeConverterTest {
    private val converter = PhraseTypeConverter()

    @Test
    fun testConvertToDatabaseColumn_MapsPhraseTypeToId() {
        assertEquals(0, converter.convertToDatabaseColumn(PhraseType.Aaron))
        assertEquals(8, converter.convertToDatabaseColumn(PhraseType.Zozo))
        assertNull(converter.convertToDatabaseColumn(null))
    }

    @Test
    fun testConvertToEntityAttribute_MapsIdToPhraseType() {
        assertEquals(PhraseType.Aaron, converter.convertToEntityAttribute(0))
        assertEquals(PhraseType.Zozo, converter.convertToEntityAttribute(8))
        assertNull(converter.convertToEntityAttribute(null))
        assertNull(converter.convertToEntityAttribute(999))
    }
}
