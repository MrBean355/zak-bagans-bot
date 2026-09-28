package com.github.mrbean355.zakbot.db

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class PhraseTypeConverter : AttributeConverter<PhraseType, Int> {

    override fun convertToDatabaseColumn(attribute: PhraseType?): Int? {
        return attribute?.id
    }

    override fun convertToEntityAttribute(dbData: Int?): PhraseType? {
        return dbData?.let(PhraseType::fromIdOrNull)
    }
}
